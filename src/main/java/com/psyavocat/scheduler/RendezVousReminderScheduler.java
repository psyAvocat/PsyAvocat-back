package com.psyavocat.scheduler;

import com.psyavocat.entity.RendezVous;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class RendezVousReminderScheduler {

    private final RendezVousRepository rendezVousRepository;
    private final NotificationService notificationService;

    public RendezVousReminderScheduler(
            RendezVousRepository rendezVousRepository,
            NotificationService notificationService
    ) {
        this.rendezVousRepository = rendezVousRepository;
        this.notificationService = notificationService;
    }

    // S'exécute toutes les heures à la minute 0 (0 * * * * *)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendReminders() {
        log.info("Exécution du cron job de rappel des rendez-vous...");
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Rappel Jour J (dans les prochaines 2 heures)
        LocalDateTime dans2Heures = now.plusHours(2);
        List<RendezVous> rdvJourJ = rendezVousRepository.findAll().stream()
                .filter(r -> "CONFIRME".equals(r.getStatut()))
                .filter(r -> r.getDateHeure().isAfter(now) && r.getDateHeure().isBefore(dans2Heures))
                .toList();

        for (RendezVous rdv : rdvJourJ) {
            String msg = "Rappel Jour-J : Votre rendez-vous approche (à " + rdv.getDateHeure().toLocalTime() + ").";
            if (rdv.getPatient() != null) {
                notificationService.sendNotification(rdv.getPatient().getId(), "RAPPEL_RDV_JOUR_J", msg, rdv.getLienVisio());
            }
            if (rdv.getProfessionnel() != null) {
                notificationService.sendNotification(rdv.getProfessionnel().getId(), "RAPPEL_RDV_JOUR_J", msg, rdv.getLienVisio());
            }
        }

        // 2. Rappel -24h
        LocalDateTime dans24Heures = now.plusHours(24);
        LocalDateTime dans25Heures = now.plusHours(25); // Fenêtre d'1h pour éviter les doublons
        List<RendezVous> rdv24h = rendezVousRepository.findAll().stream()
                .filter(r -> "CONFIRME".equals(r.getStatut()))
                .filter(r -> r.getDateHeure().isAfter(dans24Heures) && r.getDateHeure().isBefore(dans25Heures))
                .toList();

        for (RendezVous rdv : rdv24h) {
            String msg = "Rappel -24h : N'oubliez pas votre rendez-vous demain à " + rdv.getDateHeure().toLocalTime() + ".";
            if (rdv.getPatient() != null) {
                notificationService.sendNotification(rdv.getPatient().getId(), "RAPPEL_RDV_24H", msg, null);
            }
            if (rdv.getProfessionnel() != null) {
                notificationService.sendNotification(rdv.getProfessionnel().getId(), "RAPPEL_RDV_24H", msg, null);
            }
        }
    }
}
