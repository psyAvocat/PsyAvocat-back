package com.psyavocat.service.impl;

import com.psyavocat.dto.dossier.DossierResponseDTO;
import com.psyavocat.dto.dossier.SoumissionDossierResponseDTO;
import com.psyavocat.dto.seance.CreateSeanceRequest;
import com.psyavocat.entity.*;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.SoumissionDossierService;
import com.psyavocat.service.support.AccesProfessionnel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Contrôles de propriété : dossiers juridiques (cloisonnement entre confrères)
 * et séances psychologiques (patients et rendez-vous d'autrui).
 */
@ExtendWith(MockitoExtension.class)
class DossierEtSeanceAccesTest {

    @Mock private DossierRepository dossierRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private SoumissionDossierService soumissionDossierService;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private FichePatientRepository fichePatientRepository;
    @Mock private SeanceRepository seanceRepository;
    @Mock private NoteSeanceRepository noteSeanceRepository;
    @Mock private PsychologueRepository psychologueRepository;
    @Mock private RendezVousRepository rendezVousRepository;

    private DossierServiceImpl dossierService;
    private SeanceServiceImpl seanceService;

    private Avocat avocatA;
    private Avocat avocatB;
    private Client justiciable;
    private Dossier dossier;
    private Psychologue psy;
    private Psychologue autrePsy;
    private Client patient;

    @BeforeEach
    void setUp() {
        AccesProfessionnel acces = new AccesProfessionnel(authenticationContext, utilisateurRepository);
        dossierService = new DossierServiceImpl(dossierRepository, utilisateurRepository,
                soumissionDossierService, authenticationContext, acces,
                mock(PieceJointeRepository.class), mock(SpecialiteRepository.class),
                mock(com.psyavocat.storage.service.DocumentStorageService.class));
        seanceService = new SeanceServiceImpl(fichePatientRepository, seanceRepository, noteSeanceRepository,
                psychologueRepository, utilisateurRepository, rendezVousRepository, authenticationContext, acces);

        avocatA = pro(new Avocat(), "avocat-a");
        avocatB = pro(new Avocat(), "avocat-b");
        psy = pro(new Psychologue(), "psy-1");
        autrePsy = pro(new Psychologue(), "psy-2");

        justiciable = new Client();
        justiciable.setId("client-1");
        patient = new Client();
        patient.setId("patient-1");

        dossier = new Dossier();
        dossier.setId("dossier-1");
        dossier.setJusticiable(justiciable);
        dossier.setSoumissions(new ArrayList<>(List.of(soumission(avocatA), soumission(avocatB))));
    }

    @Test
    @DisplayName("Un avocat destinataire ne voit que SA soumission, jamais celle (ni le tarif) d'un confrère")
    void avocat_neVoitQueSaSoumission() {
        connecte(avocatA);
        when(dossierRepository.findById("dossier-1")).thenReturn(Optional.of(dossier));
        when(soumissionDossierService.getSoumissionsParDossier("dossier-1")).thenReturn(List.of(
                SoumissionDossierResponseDTO.builder().id("s-a").avocatId("avocat-a").tarifPropose(new BigDecimal("300")).build(),
                SoumissionDossierResponseDTO.builder().id("s-b").avocatId("avocat-b").tarifPropose(new BigDecimal("150")).build()));

        DossierResponseDTO dto = dossierService.getDossierById("dossier-1");

        assertThat(dto.getSoumissions()).extracting(SoumissionDossierResponseDTO::getAvocatId).containsExactly("avocat-a");
    }

    @Test
    @DisplayName("Le justiciable propriétaire voit toutes les réponses de son dossier")
    void proprietaire_voitToutesLesSoumissions() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(dossierRepository.findById("dossier-1")).thenReturn(Optional.of(dossier));
        when(soumissionDossierService.getSoumissionsParDossier("dossier-1")).thenReturn(List.of(
                SoumissionDossierResponseDTO.builder().id("s-a").avocatId("avocat-a").build(),
                SoumissionDossierResponseDTO.builder().id("s-b").avocatId("avocat-b").build()));

        assertThat(dossierService.getDossierById("dossier-1").getSoumissions()).hasSize(2);
    }

    @Test
    @DisplayName("Séance : rattachement au rendez-vous d'un autre psychologue refusé, rendez-vous inchangé")
    void seance_rendezVousDAutrui_refuse() {
        connecte(psy);
        when(utilisateurRepository.findById("patient-1")).thenReturn(Optional.of(patient));
        RendezVous rdvAutrui = new RendezVous();
        rdvAutrui.setId("rdv-x");
        rdvAutrui.setProfessionnel(autrePsy);
        rdvAutrui.setPatient(patient);
        rdvAutrui.setStatut("CONFIRME");
        when(rendezVousRepository.findById("rdv-x")).thenReturn(Optional.of(rdvAutrui));

        CreateSeanceRequest request = new CreateSeanceRequest("patient-1", LocalDateTime.now(), null, null, "rdv-x");

        assertThatThrownBy(() -> seanceService.createSeance(request)).isInstanceOf(ForbiddenException.class);
        assertThat(rdvAutrui.getStatut()).isEqualTo("CONFIRME");
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("Séance : fiche refusée pour un client sans fiche ni rendez-vous avec ce psychologue")
    void seance_clientSansRelation_refuse() {
        connecte(psy);
        when(utilisateurRepository.findById("patient-1")).thenReturn(Optional.of(patient));
        when(fichePatientRepository.findByPsychologueIdAndPatientId("psy-1", "patient-1")).thenReturn(Optional.empty());
        when(rendezVousRepository.existsByProfessionnelIdAndPatientId("psy-1", "patient-1")).thenReturn(false);

        CreateSeanceRequest request = new CreateSeanceRequest("patient-1", LocalDateTime.now(), null, null, null);

        assertThatThrownBy(() -> seanceService.createSeance(request)).isInstanceOf(ForbiddenException.class);
        verify(fichePatientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Séance : psychologue suspendu refusé")
    void seance_psychologueSuspendu_refuse() {
        psy.setStatutValidation("SUSPENDED");
        connecte(psy);

        CreateSeanceRequest request = new CreateSeanceRequest("patient-1", LocalDateTime.now(), null, null, null);

        assertThatThrownBy(() -> seanceService.createSeance(request))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo(AccesProfessionnel.CODE_NON_APPROUVE);
    }

    private <T extends Professionnel> T pro(T p, String id) {
        p.setId(id);
        p.setStatutValidation("APPROVED");
        return p;
    }

    private SoumissionDossier soumission(Avocat avocat) {
        SoumissionDossier s = new SoumissionDossier();
        s.setAvocat(avocat);
        s.setStatut("EN_ATTENTE");
        return s;
    }

    private void connecte(Professionnel p) {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(p.getId());
        when(utilisateurRepository.findById(p.getId())).thenReturn(Optional.of(p));
    }
}
