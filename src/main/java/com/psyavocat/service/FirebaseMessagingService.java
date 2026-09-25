package com.psyavocat.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service d'envoi de notifications Firebase Cloud Messaging (FCM) côté backend.
 *
 * Permet l'envoi vers un appareil spécifique, vers plusieurs cibles,
 * ou avec des payloads de données contextuelles (data payload).
 */
@Slf4j
@Service
public class FirebaseMessagingService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public FirebaseMessagingService(ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    /**
     * Envoie une notification à un jeton d'enregistrement FCM unique.
     *
     * @param targetToken Le token FCM de l'appareil destinataire
     * @param title Le titre de la notification
     * @param body Le corps du message
     * @param data Les données additionnelles (data payload)
     * @return L'ID du message envoyé ou null en cas d'erreur
     */
    public String sendToDevice(String targetToken, String title, String body, Map<String, String> data) {
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null) {
            log.warn("FirebaseMessaging n'est pas initialisé. Impossible d'envoyer la notification à : {}", targetToken);
            return null;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = messaging.send(messageBuilder.build());
            log.info("Notification FCM envoyée avec succès, message ID : {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Échec de l'envoi de la notification FCM au token {} : {}", targetToken, e.getMessage());
            return null;
        }
    }

    /**
     * Envoie une notification à plusieurs appareils en multicast.
     *
     * @param targetTokens Liste des tokens FCM des destinataires
     * @param title Titre
     * @param body Corps
     * @param data Données
     * @return BatchResponse contenant le nombre de succès et d'échecs
     */
    public BatchResponse sendToMultipleDevices(List<String> targetTokens, String title, String body, Map<String, String> data) {
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null || targetTokens == null || targetTokens.isEmpty()) {
            log.warn("FirebaseMessaging non disponible ou liste de tokens vide.");
            return null;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            MulticastMessage.Builder multicastBuilder = MulticastMessage.builder()
                    .addAllTokens(targetTokens)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                multicastBuilder.putAllData(data);
            }

            BatchResponse response = messaging.sendEachForMulticast(multicastBuilder.build());
            log.info("Envoi FCM multicast effectué : {} succès, {} échecs",
                    response.getSuccessCount(), response.getFailureCount());
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Échec de l'envoi multicast FCM : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Envoie un message de données pures (Data message silencieux) à un appareil.
     */
    public String sendDataMessage(String targetToken, Map<String, String> data) {
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null || data == null || data.isEmpty()) {
            return null;
        }

        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .putAllData(data)
                    .build();

            String response = messaging.send(message);
            log.info("Message de données FCM envoyé, ID : {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Échec de l'envoi du message de données FCM : {}", e.getMessage());
            return null;
        }
    }
}
