package com.psyavocat.service.impl;

import com.psyavocat.entity.Notification;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.repository.NotificationRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Suppression des notifications : une à une (propriétaire uniquement) ou toutes les lues. */
@ExtendWith(MockitoExtension.class)
class NotificationSuppressionTest {

    @Mock private NotificationRepository repository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private ApplicationEventPublisher eventPublisher;

    private NotificationServiceImpl service;

    private Notification notification(String id, String destinataireId, boolean lu) {
        Utilisateur u = new com.psyavocat.entity.Justiciable();
        u.setId(destinataireId);
        Notification n = new Notification();
        n.setId(id);
        n.setDestinataire(u);
        n.setLu(lu);
        return n;
    }

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(repository, utilisateurRepository, authenticationContext, eventPublisher);
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
    }

    @Test
    @DisplayName("Supprimer les lues : seules les notifications lues de l'utilisateur connecté")
    void supprimerLues() {
        List<Notification> lues = List.of(notification("n1", "client-1", true), notification("n2", "client-1", true));
        when(repository.findByDestinataireIdAndLuTrue("client-1")).thenReturn(lues);

        assertThat(service.supprimerLues()).isEqualTo(2);
        verify(repository).deleteAll(lues);
    }

    @Test
    @DisplayName("Supprimer la notification d'un autre utilisateur : refusé")
    void suppressionRefuseePourAutrui() {
        when(repository.findById("n3")).thenReturn(Optional.of(notification("n3", "client-2", false)));

        assertThatThrownBy(() -> service.deleteNotification("n3")).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
    }
}
