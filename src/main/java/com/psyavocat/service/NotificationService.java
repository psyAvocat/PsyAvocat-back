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

    /**
     * Marque comme lues les notifications d'un utilisateur liées à une ressource
     * (ex. les « Nouveau message » d'une conversation qu'il vient d'ouvrir).
     */
    void marquerLuesPourRessource(String destinataireId, String ressourceType, String ressourceId);

    void deleteNotification(String id);

    /** Supprime les notifications déjà lues de l'utilisateur connecté ; renvoie leur nombre. */
    int supprimerLues();
}
