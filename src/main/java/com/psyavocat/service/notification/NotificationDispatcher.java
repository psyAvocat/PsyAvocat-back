package com.psyavocat.service.notification;

import com.psyavocat.dto.notification.NotificationResponseDTO;
import com.psyavocat.realtime.RealtimeGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Diffuse une notification APRÈS validation de la transaction qui l'a créée :
 * jamais de push pour une opération finalement annulée.
 *
 * WebSocket → mise à jour immédiate si l'app est ouverte.
 * FCM → notification téléphone (premier plan, arrière-plan, app fermée).
 */
@Slf4j
@Component
public class NotificationDispatcher {

    /** Événement publié par NotificationServiceImpl après persistance. */
    public record NotificationCreee(String destinataireId, NotificationResponseDTO notification) {
    }

    private final RealtimeGateway realtimeGateway;
    private final PushNotificationService pushNotificationService;

    public NotificationDispatcher(RealtimeGateway realtimeGateway, PushNotificationService pushNotificationService) {
        this.realtimeGateway = realtimeGateway;
        this.pushNotificationService = pushNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void diffuser(NotificationCreee evenement) {
        NotificationResponseDTO n = evenement.notification();
        realtimeGateway.envoyerA(evenement.destinataireId(), "NOTIFICATION", n);

        try {
            pushNotificationService.envoyerA(evenement.destinataireId(), n.getTitre(), n.getContenu(), donneesPush(n));
        } catch (Exception e) {
            // Le push est un canal de confort : son échec n'annule jamais l'opération métier.
            log.warn("Push FCM non envoyé pour la notification {} : {}", n.getId(), e.getMessage());
        }
    }

    /** Données utilisées par Flutter pour le lien profond au clic sur la notification. */
    private Map<String, String> donneesPush(NotificationResponseDTO n) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", n.getId());
        data.put("type", valeur(n.getType()));
        data.put("univers", valeur(n.getUnivers()));
        data.put("ressourceType", valeur(n.getRessourceType()));
        data.put("ressourceId", valeur(n.getRessourceId()));
        return data;
    }

    private String valeur(String v) {
        return v != null ? v : "";
    }
}
