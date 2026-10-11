package com.psyavocat.service.impl;

import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.mapper.ProfessionnelMapper;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.*;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.ProfessionnelService;
import com.psyavocat.service.notification.RendezVousEvenements;
import com.psyavocat.service.support.JournalDecisions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Décisions administratives : effets d'une suspension, signalements, historique.
 */
@ExtendWith(MockitoExtension.class)
class DecisionsAdministrativesTest {

    @Mock private ProfessionnelRepository professionnelRepository;
    @Mock private ProfessionnelMapper professionnelMapper;
    @Mock private NotificationService notificationService;
    @Mock private RealtimeGateway realtimeGateway;
    @Mock private RendezVousRepository rendezVousRepository;
    @Mock private DisponibiliteRepository disponibiliteRepository;
    @Mock private RendezVousEvenements rendezVousEvenements;
    @Mock private JournalDecisions journalDecisions;

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private SignalementRepository signalementRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private JusticiableRepository justiciableRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ProfessionnelService professionnelServiceMock;

    private ProfessionnelServiceImpl professionnelService;
    private AdminServiceImpl adminService;
    private Psychologue psy;

    @BeforeEach
    void setUp() {
        professionnelService = new ProfessionnelServiceImpl(professionnelRepository, professionnelMapper,
                notificationService, realtimeGateway, rendezVousRepository, disponibiliteRepository,
                rendezVousEvenements, journalDecisions);
        adminService = new AdminServiceImpl(utilisateurRepository, signalementRepository, patientRepository,
                justiciableRepository, clientRepository, professionnelServiceMock, notificationService,
                journalDecisions, realtimeGateway);
        psy = new Psychologue();
        psy.setId("psy-1");
        psy.setStatutValidation("APPROVED");
    }

    @Test
    @DisplayName("Suspension sans motif refusée")
    void suspension_sansMotif_refusee() {
        assertThatThrownBy(() -> professionnelService.updateStatutValidation("psy-1", "SUSPENDED", "  "))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(professionnelRepository);
    }

    @Test
    @DisplayName("Suspension : rendez-vous futurs annulés (créneau libéré, acompte annulé, client prévenu), passé intact, décision historisée")
    void suspension_annuleLesRendezVousFuturs() {
        when(professionnelRepository.findById("psy-1")).thenReturn(Optional.of(psy));
        when(professionnelRepository.save(psy)).thenReturn(psy);

        Disponibilite creneau = new Disponibilite();
        creneau.setStatut("RESERVE");
        Paiement paiement = new Paiement();
        paiement.setStatut("PAYE");
        RendezVous futur = rdv("CONFIRME", LocalDateTime.now().plusDays(2));
        futur.setDisponibilite(creneau);
        futur.setPaiement(paiement);
        RendezVous passe = rdv("PASSE", LocalDateTime.now().minusDays(2));
        when(rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc("psy-1")).thenReturn(List.of(futur, passe));

        professionnelService.updateStatutValidation("psy-1", "SUSPENDED", "Plaintes répétées");

        assertThat(psy.getStatutValidation()).isEqualTo("SUSPENDED");
        assertThat(futur.getStatut()).isEqualTo("ANNULE");
        assertThat(creneau.getStatut()).isEqualTo("LIBRE");
        assertThat(paiement.getStatut()).isEqualTo("ANNULE");
        assertThat(passe.getStatut()).isEqualTo("PASSE");
        verify(rendezVousEvenements).rendezVousAnnuleParPlateforme(futur);
        verify(rendezVousEvenements, never()).rendezVousAnnuleParPlateforme(passe);
        verify(journalDecisions).enregistrer(eq(JournalDecisions.TYPE_STATUT_PROFESSIONNEL), eq("psy-1"),
                eq("SUSPENDED"), eq("Plaintes répétées"));
        verify(realtimeGateway).envoyerA(eq("psy-1"), eq("COMPTE_MIS_A_JOUR"), any());
        verify(notificationService).sendNotification(eq("psy-1"), eq("COMPTE_SUSPENDU"), contains("Plaintes répétées"), isNull());
    }

    @Test
    @DisplayName("Signalement : suspension sans motif refusée, aucun effet")
    void signalement_suspensionSansMotif_refusee() {
        Signalement s = signalement();
        when(signalementRepository.findById("sig-1")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> adminService.traiterSignalement("sig-1", "SUSPENDRE", null))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(professionnelServiceMock);
        assertThat(s.getStatut()).isEqualTo("EN_ATTENTE");
    }

    @Test
    @DisplayName("Signalement : suspension via le workflow standard, décision historisée, auteur notifié")
    void signalement_suspension_workflowComplet() {
        Signalement s = signalement();
        when(signalementRepository.findById("sig-1")).thenReturn(Optional.of(s));
        when(signalementRepository.save(s)).thenReturn(s);

        adminService.traiterSignalement("sig-1", "SUSPENDRE", "Comportement inapproprié");

        verify(professionnelServiceMock).updateStatutValidation("psy-1", "SUSPENDED", "Comportement inapproprié");
        assertThat(s.getStatut()).isEqualTo("TRAITE_SUSPENDU");
        verify(journalDecisions).enregistrer(JournalDecisions.TYPE_SIGNALEMENT, "sig-1", "TRAITE_SUSPENDU", "Comportement inapproprié");
        verify(notificationService).notifier(argThat(n -> n.destinataireId().equals("client-1")
                && n.type().equals("SIGNALEMENT_TRAITE")));
    }

    @Test
    @DisplayName("Un signalement déjà traité ne peut pas être retraité")
    void signalement_dejaTraite_refuse() {
        Signalement s = signalement();
        s.setStatut("TRAITE_CLASSE");
        when(signalementRepository.findById("sig-1")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> adminService.traiterSignalement("sig-1", "CLASSER", null))
                .isInstanceOf(BadRequestException.class);
    }

    private RendezVous rdv(String statut, LocalDateTime date) {
        RendezVous r = new RendezVous();
        r.setStatut(statut);
        r.setDateHeure(date);
        r.setProfessionnel(psy);
        return r;
    }

    private Signalement signalement() {
        Client auteur = new Client();
        auteur.setId("client-1");
        Signalement s = new Signalement();
        s.setId("sig-1");
        s.setStatut("EN_ATTENTE");
        s.setAuteur(auteur);
        s.setUtilisateurVise(psy);
        return s;
    }
}
