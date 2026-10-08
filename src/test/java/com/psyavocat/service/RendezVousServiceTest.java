package com.psyavocat.service;

import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousPsyRequest;
import com.psyavocat.dto.rendezvous.RendezVousResponseDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.TarifProfessionnel;
import com.psyavocat.entity.Disponibilite;
import com.psyavocat.entity.Dossier;
import com.psyavocat.entity.Justiciable;
import com.psyavocat.entity.Paiement;
import com.psyavocat.entity.Patient;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.SoumissionDossier;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.repository.AvocatRepository;
import com.psyavocat.repository.DisponibiliteRepository;
import com.psyavocat.repository.DossierRepository;
import com.psyavocat.repository.PsychologueRepository;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.repository.SoumissionDossierRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.impl.RendezVousServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RendezVousServiceTest {

    @Mock
    private RendezVousRepository rendezVousRepository;

    @Mock
    private DisponibiliteRepository disponibiliteRepository;

    @Mock
    private PsychologueRepository psychologueRepository;

    @Mock
    private AvocatRepository avocatRepository;

    @Mock
    private SoumissionDossierRepository soumissionDossierRepository;

    @Mock
    private DossierRepository dossierRepository;

    @Mock
    private PaiementService paiementService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private com.psyavocat.service.notification.RendezVousEvenements evenements;

    @Mock
    private com.psyavocat.mapper.MediaUrlResolver mediaUrlResolver;

    private RendezVousServiceImpl rendezVousService;

    private Utilisateur patient;
    private Psychologue psychologue;
    private Avocat avocat;
    private Disponibilite disponibilite;

    @BeforeEach
    void setUp() {
        rendezVousService = new RendezVousServiceImpl(
                rendezVousRepository,
                disponibiliteRepository,
                psychologueRepository,
                avocatRepository,
                soumissionDossierRepository,
                dossierRepository,
                paiementService,
                utilisateurRepository,
                authenticationContext,
                evenements,
                mediaUrlResolver
        );

        patient = new Patient();
        patient.setId("patient-uid-123");
        patient.setNom("Benali");
        patient.setPrenom("Karim");

        psychologue = new Psychologue();
        psychologue.setId("psy-uid-456");
        psychologue.setNom("Moreau");
        psychologue.setPrenom("Claire");
        psychologue.setStatutValidation("APPROVED");
        TarifProfessionnel tarifPsy = new TarifProfessionnel();
        tarifPsy.setMontant(new BigDecimal("80.00"));
        tarifPsy.setActif(true);
        psychologue.getTarifs().add(tarifPsy);

        avocat = new Avocat();
        avocat.setId("avocat-uid-789");
        avocat.setNom("Dupont");
        avocat.setPrenom("Jean");
        avocat.setStatutValidation("APPROVED");

        disponibilite = new Disponibilite();
        disponibilite.setId("disp-1");
        disponibilite.setDate(LocalDate.now().plusDays(2));
        disponibilite.setHeureDebut(LocalTime.of(14, 0));
        disponibilite.setHeureFin(LocalTime.of(15, 0));
        disponibilite.setStatut("LIBRE");
        disponibilite.setProfessionnel(psychologue);
    }

    @Test
    @DisplayName("Prise de RDV direct psychologue : créneau libre réservé, calcul acompte 20%, confirmation directe")
    void testCreateRendezVousPsychologue_SuccesDirectSansValidationPraticien() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(utilisateurRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(psychologueRepository.findById(psychologue.getId())).thenReturn(Optional.of(psychologue));
        when(disponibiliteRepository.findByIdForUpdate(disponibilite.getId())).thenReturn(Optional.of(disponibilite));

        BigDecimal montantTotal = new BigDecimal("80.00");
        BigDecimal montantAcompte = new BigDecimal("16.00");
        when(paiementService.calculerAcompteRendezVous(montantTotal)).thenReturn(montantAcompte);

        Paiement paiementSimule = new Paiement();
        paiementSimule.setId("pay-1");
        paiementSimule.setMontant(montantAcompte);
        paiementSimule.setStatut("PAYE");
        when(paiementService.traiterAcompteRendezVous(eq(patient), any(RendezVous.class), eq(montantAcompte)))
                .thenReturn(paiementSimule);

        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(invocation -> {
            RendezVous r = invocation.getArgument(0);
            r.setId("rdv-created-1");
            return r;
        });

        CreateRendezVousPsyRequest request = new CreateRendezVousPsyRequest();
        request.setPsychologueId(psychologue.getId());
        request.setDisponibiliteId(disponibilite.getId());
        request.setMontantTotal(montantTotal);
        request.setMode("VISIO");

        RendezVousResponseDTO response = rendezVousService.createRendezVousPsychologue(request);

        assertThat(response).isNotNull();
        // Vérification de la règle absolue : pas d'étape "EN_ATTENTE_VALIDATION", confirmation directe
        assertThat(response.getStatut()).isEqualTo("CONFIRME");
        assertThat(disponibilite.getStatut()).isEqualTo("RESERVE");
        assertThat(response.getMontantAcompte()).isEqualByComparingTo(montantAcompte);
        assertThat(response.getStatutPaiement()).isEqualTo("PAYE");

        verify(disponibiliteRepository).save(disponibilite);
        verify(paiementService).traiterAcompteRendezVous(eq(patient), any(RendezVous.class), eq(montantAcompte));
    }

    @Test
    @DisplayName("Refus de réservation si le créneau est déjà RESERVE (empêchement double réservation)")
    void testCreateRendezVous_CreneauNonDisponible_Rejet() {
        disponibilite.setStatut("RESERVE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(utilisateurRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(psychologueRepository.findById(psychologue.getId())).thenReturn(Optional.of(psychologue));
        when(disponibiliteRepository.findByIdForUpdate(disponibilite.getId())).thenReturn(Optional.of(disponibilite));

        CreateRendezVousPsyRequest request = new CreateRendezVousPsyRequest();
        request.setPsychologueId(psychologue.getId());
        request.setDisponibiliteId(disponibilite.getId());
        request.setMontantTotal(new BigDecimal("80.00"));

        assertThatThrownBy(() -> rendezVousService.createRendezVousPsychologue(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est plus disponible");

        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("Prise de RDV avocat : nécessite soumission ACCEPTEE, confirme directement")
    void testCreateRendezVousAvocat_SuccesApresAcceptationSoumission() {
        disponibilite.setProfessionnel(avocat);

        Justiciable justiciable = new Justiciable();
        justiciable.setId("justiciable-uid-123");
        justiciable.setNom("Benali");
        justiciable.setPrenom("Karim");

        Dossier dossier = new Dossier();
        dossier.setId("dossier-1");
        dossier.setJusticiable(justiciable);

        SoumissionDossier soumission = new SoumissionDossier();
        soumission.setId("soum-1");
        soumission.setDossier(dossier);
        soumission.setAvocat(avocat);
        soumission.setStatut("ACCEPTEE");
        soumission.setTarifPropose(new BigDecimal("200.00"));

        SoumissionDossier autreSoumission = new SoumissionDossier();
        autreSoumission.setId("soum-confrere-2");
        autreSoumission.setDossier(dossier);
        autreSoumission.setStatut("EN_ATTENTE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(justiciable.getId());
        when(utilisateurRepository.findById(justiciable.getId())).thenReturn(Optional.of(justiciable));
        when(soumissionDossierRepository.findById(soumission.getId())).thenReturn(Optional.of(soumission));
        when(disponibiliteRepository.findByIdForUpdate(disponibilite.getId())).thenReturn(Optional.of(disponibilite));
        when(soumissionDossierRepository.findByDossierId(dossier.getId())).thenReturn(List.of(soumission, autreSoumission));

        BigDecimal acompte = new BigDecimal("40.00");
        when(paiementService.calculerAcompteRendezVous(soumission.getTarifPropose())).thenReturn(acompte);

        Paiement paiement = new Paiement();
        paiement.setMontant(acompte);
        paiement.setStatut("PAYE");
        when(paiementService.traiterAcompteRendezVous(eq(justiciable), any(RendezVous.class), eq(acompte)))
                .thenReturn(paiement);

        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(invocation -> {
            RendezVous r = invocation.getArgument(0);
            r.setId("rdv-avocat-1");
            return r;
        });

        CreateRendezVousAvocatRequest request = new CreateRendezVousAvocatRequest();
        request.setSoumissionId(soumission.getId());
        request.setDisponibiliteId(disponibilite.getId());
        request.setMode("CABINET");

        RendezVousResponseDTO response = rendezVousService.createRendezVousAvocat(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatut()).isEqualTo("CONFIRME");
        assertThat(disponibilite.getStatut()).isEqualTo("RESERVE");
        assertThat(response.getMontantAcompte()).isEqualByComparingTo(acompte);

        // Vérification de la Solution 2 : exclusivité et auto-invalidation
        assertThat(soumission.getStatut()).isEqualTo("RETENUE");
        assertThat(dossier.getStatut()).isEqualTo("PRIS_EN_CHARGE");
        assertThat(autreSoumission.getStatut()).isEqualTo("CADUQUE");

        verify(dossierRepository).save(dossier);
        verify(soumissionDossierRepository).save(autreSoumission);
    }

    @Test
    @DisplayName("RDV avocat rejeté si le dossier a déjà été pris en charge par un confrère")
    void testCreateRendezVousAvocat_DossierDejaPrisEnCharge_Rejet() {
        Dossier dossierPris = new Dossier();
        dossierPris.setId("dossier-pris");
        dossierPris.setStatut("PRIS_EN_CHARGE");

        SoumissionDossier soumission = new SoumissionDossier();
        soumission.setId("soum-1");
        soumission.setStatut("ACCEPTEE");
        soumission.setDossier(dossierPris);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(utilisateurRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(soumissionDossierRepository.findById(soumission.getId())).thenReturn(Optional.of(soumission));

        CreateRendezVousAvocatRequest request = new CreateRendezVousAvocatRequest();
        request.setSoumissionId(soumission.getId());
        request.setDisponibiliteId("disp-1");

        assertThatThrownBy(() -> rendezVousService.createRendezVousAvocat(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà été pris en charge");
    }

    @Test
    @DisplayName("RDV avocat rejeté si la soumission est encore EN_ATTENTE")
    void testCreateRendezVousAvocat_SoumissionEnAttente_Rejet() {
        SoumissionDossier soumission = new SoumissionDossier();
        soumission.setId("soum-1");
        soumission.setStatut("EN_ATTENTE");

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(utilisateurRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(soumissionDossierRepository.findById(soumission.getId())).thenReturn(Optional.of(soumission));

        CreateRendezVousAvocatRequest request = new CreateRendezVousAvocatRequest();
        request.setSoumissionId(soumission.getId());
        request.setDisponibiliteId("disp-1");

        assertThatThrownBy(() -> rendezVousService.createRendezVousAvocat(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nécessite que la soumission du dossier soit acceptée");
    }

    @Test
    @DisplayName("Réservation refusée si le montant ne correspond à aucun tarif publié")
    void testCreateRendezVousPsychologue_MontantHorsTarif_Rejet() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(utilisateurRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(psychologueRepository.findById(psychologue.getId())).thenReturn(Optional.of(psychologue));

        CreateRendezVousPsyRequest request = new CreateRendezVousPsyRequest();
        request.setPsychologueId(psychologue.getId());
        request.setDisponibiliteId(disponibilite.getId());
        request.setMontantTotal(new BigDecimal("1.00"));

        assertThatThrownBy(() -> rendezVousService.createRendezVousPsychologue(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("aucun tarif");
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un professionnel ne peut pas réserver comme un client")
    void testCreateRendezVous_ParProfessionnel_Forbidden() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(avocat.getId());
        when(utilisateurRepository.findById(avocat.getId())).thenReturn(Optional.of(avocat));

        CreateRendezVousPsyRequest request = new CreateRendezVousPsyRequest();
        request.setPsychologueId(psychologue.getId());
        request.setDisponibiliteId(disponibilite.getId());
        request.setMontantTotal(new BigDecimal("80.00"));

        assertThatThrownBy(() -> rendezVousService.createRendezVousPsychologue(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Modification : le RDV passe sur le nouveau créneau, l'ancien est libéré, les parties sont notifiées")
    void testModifierCreneau_Succes() {
        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");
        rdv.setStatut("CONFIRME");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);
        disponibilite.setStatut("RESERVE");
        rdv.setDisponibilite(disponibilite);
        rdv.setDateHeure(LocalDateTime.of(disponibilite.getDate(), disponibilite.getHeureDebut()));

        Disponibilite nouveau = new Disponibilite();
        nouveau.setId("disp-2");
        nouveau.setDate(LocalDate.now().plusDays(3));
        nouveau.setHeureDebut(LocalTime.of(10, 0));
        nouveau.setHeureFin(LocalTime.of(11, 0));
        nouveau.setStatut("LIBRE");
        nouveau.setProfessionnel(psychologue);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(rendezVousRepository.findById("rdv-1")).thenReturn(Optional.of(rdv));
        when(disponibiliteRepository.findByIdForUpdate("disp-2")).thenReturn(Optional.of(nouveau));
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RendezVousResponseDTO dto = rendezVousService.modifierCreneau("rdv-1",
                new com.psyavocat.dto.rendezvous.ModifierCreneauRequest("disp-2"));

        assertThat(dto.getDisponibiliteId()).isEqualTo("disp-2");
        assertThat(dto.getDureeMinutes()).isEqualTo(60);
        assertThat(nouveau.getStatut()).isEqualTo("RESERVE");
        assertThat(disponibilite.getStatut()).isEqualTo("LIBRE");
        verify(evenements).rendezVousModifie(rdv);
    }

    @Test
    @DisplayName("Modification refusée si le nouveau créneau vient d'être pris (conflit)")
    void testModifierCreneau_CreneauDejaPris_Rejet() {
        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");
        rdv.setStatut("CONFIRME");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);
        rdv.setDisponibilite(disponibilite);
        rdv.setDateHeure(LocalDateTime.of(disponibilite.getDate(), disponibilite.getHeureDebut()));

        Disponibilite pris = new Disponibilite();
        pris.setId("disp-2");
        pris.setDate(LocalDate.now().plusDays(3));
        pris.setHeureDebut(LocalTime.of(10, 0));
        pris.setHeureFin(LocalTime.of(11, 0));
        pris.setStatut("RESERVE");
        pris.setProfessionnel(psychologue);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(rendezVousRepository.findById("rdv-1")).thenReturn(Optional.of(rdv));
        when(disponibiliteRepository.findByIdForUpdate("disp-2")).thenReturn(Optional.of(pris));

        assertThatThrownBy(() -> rendezVousService.modifierCreneau("rdv-1",
                new com.psyavocat.dto.rendezvous.ModifierCreneauRequest("disp-2")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est plus disponible");
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("Annulation autorisée par le patient")
    void testAnnulerRendezVous_ParPatient_Succes() {
        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");
        rdv.setStatut("CONFIRME");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(rendezVousRepository.findById(rdv.getId())).thenReturn(Optional.of(rdv));
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RendezVousResponseDTO dto = rendezVousService.annulerRendezVous(rdv.getId());

        assertThat(dto.getStatut()).isEqualTo("ANNULE");
        verify(rendezVousRepository).save(rdv);
        verify(evenements).rendezVousAnnule(rdv, patient.getId());
    }

    @Test
    @DisplayName("Annulation autorisée par le praticien")
    void testAnnulerRendezVous_ParProfessionnel_Succes() {
        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");
        rdv.setStatut("CONFIRME");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(psychologue.getId());
        when(rendezVousRepository.findById(rdv.getId())).thenReturn(Optional.of(rdv));
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RendezVousResponseDTO dto = rendezVousService.annulerRendezVous(rdv.getId());

        assertThat(dto.getStatut()).isEqualTo("ANNULE");
        verify(rendezVousRepository).save(rdv);
    }

    @Test
    @DisplayName("Annulation refusée pour un tiers non participant au RDV")
    void testAnnulerRendezVous_ParTiers_Forbidden() {
        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("tiers-inconnu-999");
        when(rendezVousRepository.findById(rdv.getId())).thenReturn(Optional.of(rdv));

        assertThatThrownBy(() -> rendezVousService.annulerRendezVous(rdv.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("pas accès à ce rendez-vous");

        verify(rendezVousRepository, never()).save(any());
    }
}
