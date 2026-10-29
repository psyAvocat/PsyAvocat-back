package com.psyavocat.service.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Exécute une action une fois la transaction en cours validée (ou tout de suite, hors transaction).
 *
 * Sert aux événements temps réel : un client qui reçoit « nouveau message » relit aussitôt
 * l'API ; envoyé avant la validation, l'événement le ferait relire un état où le message
 * n'existe pas encore (message absent jusqu'à l'événement suivant). Annulée si la
 * transaction échoue : aucun événement pour un message qui n'a pas été enregistré.
 */
public final class ApresValidation {

    private ApresValidation() {
    }

    public static void executer(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
