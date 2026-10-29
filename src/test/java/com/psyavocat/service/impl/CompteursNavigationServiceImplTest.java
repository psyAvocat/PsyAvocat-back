package com.psyavocat.service.impl;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.repository.MessageContactRepository;
import com.psyavocat.repository.NotificationRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Badges de la barre latérale de l'espace professionnel. */
class CompteursNavigationServiceImplTest {

    private AuthenticationContext auth;
    private UtilisateurRepository utilisateurs;
    private NotificationRepository notifications;
    private MessageContactRepository messages;
    private CompteursNavigationServiceImpl service;

    @BeforeEach
    void setUp() {
        auth = mock(AuthenticationContext.class);
        utilisateurs = mock(UtilisateurRepository.class);
        notifications = mock(NotificationRepository.class);
        messages = mock(MessageContactRepository.class);
        service = new CompteursNavigationServiceImpl(auth, utilisateurs, notifications, messages);
    }

    @Test
    @DisplayName("Avocat : nouveaux dossiers reçus, nouveaux rendez-vous et messages non lus (événements réels)")
    void avocat() {
        Avocat avocat = new Avocat();
        avocat.setId("avocat-1");
        when(auth.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurs.findById("avocat-1")).thenReturn(Optional.of(avocat));
        when(notifications.countByDestinataireIdAndRessourceTypeAndLuFalse("avocat-1", "SOUMISSION")).thenReturn(3L);
        when(notifications.countByDestinataireIdAndRessourceTypeAndLuFalse("avocat-1", "RENDEZ_VOUS")).thenReturn(2L);
        when(messages.countUnreadMessagesForUser("avocat-1")).thenReturn(5L);

        var compteurs = service.getCompteurs();

        assertThat(compteurs.getDossiersNouveaux()).isEqualTo(3);
        assertThat(compteurs.getRendezVousNouveaux()).isEqualTo(2);
        assertThat(compteurs.getMessagesNonLus()).isEqualTo(5);
    }

    @Test
    @DisplayName("Psychologue : aucun badge Dossiers (pas de dossiers dans cet univers)")
    void psychologue() {
        Psychologue psy = new Psychologue();
        psy.setId("psy-1");
        when(auth.getRequiredFirebaseUid()).thenReturn("psy-1");
        when(utilisateurs.findById("psy-1")).thenReturn(Optional.of(psy));

        var compteurs = service.getCompteurs();

        assertThat(compteurs.getDossiersNouveaux()).isZero();
        verify(notifications, never()).countByDestinataireIdAndRessourceTypeAndLuFalse(any(), eq("SOUMISSION"));
    }
}
