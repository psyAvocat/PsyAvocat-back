package com.psyavocat.service;

import com.psyavocat.dto.notification.NotificationResponseDTO;
import com.psyavocat.service.notification.NotificationEvenement;

import java.util.List;

public interface NotificationService {
    List<NotificationResponseDTO> getMesNotifications();

    /**
     * Crée une notification métier : persistance MySQL, puis (après validation de la
     * transaction) envoi WebSocket et push FCM vers tous les appareils du destinataire.
     * Sans effet si le destinataire n'existe pas.
     */
    void notifier(NotificationEvenement evenement);

    /** Forme historique (conservée pour les appelants existants). */
    void sendNotification(String destinataireId, String type, String contenu, String lienVisio);

    long countUnread();

    void markAsRead(String id);

    void markAllAsRead();

    void deleteNotification(String id);
}
