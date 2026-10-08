package com.psyavocat.service.notification;

/**
 * Notification métier à créer pour un destinataire.
 *
 * @param destinataireId utilisateur destinataire (Firebase UID)
 * @param type           type métier (RDV_CONFIRME, RDV_ANNULE, NOUVEAU_MESSAGE, ...)
 * @param titre          titre court (liste + push)
 * @param message        texte complet
 * @param univers        AVOCAT, PSYCHOLOGUE, ou null si transverse
 * @param ressourceType  RENDEZ_VOUS, CONVERSATION, ARTICLE, CONSEIL, PROFESSIONNEL... (lien profond)
 * @param ressourceId    identifiant de la ressource ciblée
 * @param lienVisio      lien de visioconférence éventuel
 */
public record NotificationEvenement(
        String destinataireId,
        String type,
        String titre,
        String message,
        String univers,
        String ressourceType,
        String ressourceId,
        String lienVisio
) {
}
