package com.psyavocat.service.impl;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.JustificatifProfessionnel;
import com.psyavocat.repository.AdministrateurRepository;
import com.psyavocat.repository.JustificatifProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.service.support.JournalDecisions;
import com.psyavocat.storage.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Nouvelle version d'un document déjà validé (ex. CV) : vérifiée par l'administration avant de remplacer l'ancienne. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JustificatifMiseAJourTest {

    @Mock private JustificatifProfessionnelRepository repository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private DocumentStorageService storage;
    @Mock private NotificationService notificationService;
    @Mock private AdministrateurRepository administrateurRepository;

    private JustificatifServiceImpl service;
    private Avocat avocat;
    private JustificatifProfessionnel cvActuel;
    private JustificatifProfessionnel nouveauCv;

    private JustificatifProfessionnel document(String id, String statut, String fichier) {
        JustificatifProfessionnel j = new JustificatifProfessionnel();
        j.setId(id);
        j.setTypeDocument("CV");
        j.setStatutValidation(statut);
        j.setNomFichier(fichier);
        j.setCleObjet("justificatifs/professionnel/avocat-1/" + id + ".pdf");
        j.setCheminStockage("r2://" + j.getCleObjet());
        j.setProfessionnel(avocat);
        return j;
    }

    @BeforeEach
    void setUp() {
        service = new JustificatifServiceImpl(repository, utilisateurRepository, authenticationContext, storage,
                new AccesProfessionnel(authenticationContext, utilisateurRepository), notificationService,
                mock(JournalDecisions.class), administrateurRepository);
        avocat = new Avocat();
        avocat.setId("avocat-1");
        avocat.setNom("Diallo");
        avocat.setPrenom("Awa");
        avocat.setStatutValidation("APPROVED");
        cvActuel = document("cv-actuel", "VALIDE", "cv-2025.pdf");
        nouveauCv = document("cv-nouveau", "EN_ATTENTE", "cv-2026.pdf");

        when(authenticationContext.getCurrentAuthorities())
                .thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATEUR")));
        when(storage.isAvailable()).thenReturn(true);
        when(repository.findById("cv-nouveau")).thenReturn(Optional.of(nouveauCv));
        when(repository.save(any(JustificatifProfessionnel.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.findByProfessionnelIdAndTypeDocumentAndStatutValidation("avocat-1", "CV", "VALIDE"))
                .thenReturn(List.of(cvActuel));
    }

    @Test
    @DisplayName("Nouvelle version validée : elle remplace l'ancienne, le professionnel est prévenu")
    void validee_remplaceLAncienne() {
        service.decider("cv-nouveau", "VALIDE", null);

        assertThat(nouveauCv.getStatutValidation()).isEqualTo("VALIDE");
        verify(storage).deleteDocument(cvActuel.getCleObjet());
        verify(repository).delete(cvActuel);
        verify(notificationService).sendNotification(eq("avocat-1"), eq("JUSTIFICATIF_VALIDE"),
                contains("remplace la précédente"), isNull());
    }

    @Test
    @DisplayName("Nouvelle version refusée : l'ancienne reste en vigueur")
    void refusee_ancienneConservee() {
        service.decider("cv-nouveau", "REJETE", "Document illisible");

        assertThat(nouveauCv.getStatutValidation()).isEqualTo("REJETE");
        verify(repository, never()).delete(cvActuel);
        verify(storage, never()).deleteDocument(cvActuel.getCleObjet());
        verify(notificationService).sendNotification(eq("avocat-1"), eq("JUSTIFICATIF_REJETE"),
                contains("version actuelle reste en vigueur"), isNull());
    }

    @Test
    @DisplayName("Justificatif complémentaire validé : il s'ajoute sans retirer les autres")
    void complementaire_neRemplacePas() {
        JustificatifProfessionnel attestation = document("attestation", "EN_ATTENTE", "attestation.pdf");
        attestation.setTypeDocument("JUSTIFICATIF_PROFESSIONNEL");
        when(repository.findById("attestation")).thenReturn(Optional.of(attestation));
        JustificatifProfessionnel autre = document("autre", "VALIDE", "autre.pdf");
        autre.setTypeDocument("JUSTIFICATIF_PROFESSIONNEL");
        when(repository.findByProfessionnelIdAndTypeDocumentAndStatutValidation("avocat-1", "JUSTIFICATIF_PROFESSIONNEL", "VALIDE"))
                .thenReturn(List.of(autre));

        service.decider("attestation", "VALIDE", null);

        verify(repository, never()).delete(any(JustificatifProfessionnel.class));
        verify(notificationService).sendNotification(eq("avocat-1"), eq("JUSTIFICATIF_VALIDE"),
                contains("a été validé"), isNull());
    }

    @Test
    @DisplayName("Liste admin : mise à jour en attente avec le document qu'elle remplacera")
    void listeAdmin_misesAJour() {
        when(repository.findByStatutValidationAndProfessionnelStatutValidationOrderByDateDepotAsc("EN_ATTENTE", "APPROVED"))
                .thenReturn(List.of(nouveauCv));

        var liste = service.getMisesAJourEnAttente();

        assertThat(liste).hasSize(1);
        assertThat(liste.get(0).getProfessionnelNom()).isEqualTo("Awa Diallo");
        assertThat(liste.get(0).getRemplaceNomFichier()).isEqualTo("cv-2025.pdf");
    }

    @Test
    @DisplayName("Le professionnel voit le motif de refus de ses documents (et tous ses documents)")
    void professionnel_voitLeMotifDeRefus() {
        JournalDecisions journal = mock(JournalDecisions.class);
        JustificatifServiceImpl serviceAvecJournal = new JustificatifServiceImpl(repository, utilisateurRepository,
                authenticationContext, storage, new AccesProfessionnel(authenticationContext, utilisateurRepository),
                notificationService, journal, administrateurRepository);
        JustificatifProfessionnel refuse = document("cv-refuse", "REJETE", "cv-flou.pdf");
        var date = java.time.LocalDateTime.of(2026, 10, 20, 9, 30);
        when(journal.derniere("cv-refuse")).thenReturn(com.psyavocat.dto.admin.DecisionAdministrativeDTO.builder()
                .decision("REJETE").motif("Document illisible").dateDecision(date).build());
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(repository.findByProfessionnelId("avocat-1")).thenReturn(List.of(cvActuel, nouveauCv, refuse));

        var liste = serviceAvecJournal.getMyJustificatifs();

        assertThat(liste).hasSize(3);
        var dtoRefuse = liste.stream().filter(d -> d.getId().equals("cv-refuse")).findFirst().orElseThrow();
        assertThat(dtoRefuse.getMotifRejet()).isEqualTo("Document illisible");
        assertThat(dtoRefuse.getDateDecision()).isEqualTo(date);
        // Aucun motif pour un document en attente ou validé.
        assertThat(liste.stream().filter(d -> !d.getId().equals("cv-refuse")))
                .allSatisfy(d -> assertThat(d.getMotifRejet()).isNull());
    }
}
