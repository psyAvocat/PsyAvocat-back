package com.psyavocat.scheduler;

import com.psyavocat.entity.RendezVous;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.service.notification.RendezVousEvenements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Rappels de rendez-vous planifiés côté serveur (indépendants de l'application ouverte
 * et de l'univers affiché) : notification persistée + push sur tous les appareils.
 *
 * Exécution toutes les heures ; chaque fenêtre fait exactement une heure,
 * donc chaque rendez-vous reçoit chaque rappel une seule fois.
 */
@Slf4j
@Component
public class RendezVousReminderScheduler {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH'h'mm");

    private final RendezVousRepository rendezVousRepository;
    private final RendezVousEvenements evenements;

    public RendezVousReminderScheduler(RendezVousRepository rendezVousRepository,
                                       RendezVousEvenements evenements) {
        this.rendezVousRepository = rendezVousRepository;
        this.evenements = evenements;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Rappel J-1 : rendez-vous débutant entre +24 h et +25 h.
        for (RendezVous rdv : confirmesEntre(now.plusHours(24), now.plusHours(25))) {
            evenements.rappel(rdv, "RAPPEL_RDV_24H", "Rappel : rendez-vous demain",
                    "N'oubliez pas votre rendez-vous demain à " + rdv.getDateHeure().format(HEURE) + ".");
        }

        // Rappel jour J : rendez-vous débutant entre +1 h et +2 h.
        for (RendezVous rdv : confirmesEntre(now.plusHours(1), now.plusHours(2))) {
            evenements.rappel(rdv, "RAPPEL_RDV_JOUR_J", "Rappel : rendez-vous aujourd'hui",
                    "Votre rendez-vous commence à " + rdv.getDateHeure().format(HEURE) + ".");
        }
    }

    private List<RendezVous> confirmesEntre(LocalDateTime debut, LocalDateTime fin) {
        return rendezVousRepository.findByStatutAndDateHeureGreaterThanEqualAndDateHeureLessThan("CONFIRME", debut, fin);
    }
}
