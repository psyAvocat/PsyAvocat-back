package com.psyavocat.service.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Les événements temps réel partent une fois les données enregistrées, jamais avant. */
class ApresValidationTest {

    @AfterEach
    void nettoyer() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Dans une transaction : exécuté seulement après la validation")
    void attendLaValidation() {
        AtomicInteger envois = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();

        ApresValidation.executer(envois::incrementAndGet);
        assertThat(envois).hasValue(0);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(envois).hasValue(1);
    }

    @Test
    @DisplayName("Transaction annulée : rien n'est envoyé")
    void annulee_rienNEstEnvoye() {
        AtomicInteger envois = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();

        ApresValidation.executer(envois::incrementAndGet);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(envois).hasValue(0);
    }

    @Test
    @DisplayName("Hors transaction : exécuté immédiatement")
    void horsTransaction_immediat() {
        AtomicInteger envois = new AtomicInteger();

        ApresValidation.executer(envois::incrementAndGet);

        assertThat(envois).hasValue(1);
    }
}
