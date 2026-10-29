package com.psyavocat.scheduler;

import com.psyavocat.entity.RendezVous;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.service.notification.RendezVousEvenements;
import com.psyavocat.service.support.SallesVisio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rappels de rendez-vous planifiés côté serveur (indépendants de l'application ouverte
 * et de l'univers affiché) : notification persistée + temps réel + push, au client et au
 * professionnel, chacun avec le nom de l'autre partie.
 *
 * Toutes les 5 minutes ; chaque rappel est mémorisé sur le rendez-vous et ne part qu'une fois :
 * - la veille : entre 24 h et 20 h avant ;
 * - le jour même : dans les 2 heures qui précèdent ;
 * - visioconférence : dans le quart d'heure qui précède, avec le lien de la salle.
 * Un rappel plus proche du rendez-vous rend les précédents inutiles (ils ne partent plus).
 */
@Slf4j
@Component
public class RendezVousReminderScheduler {

    static final Duration DEBUT_VEILLE = Duration.ofHours(20);
    static final Duration FENETRE_JOUR = Duration.ofHours(2);
    static final Duration FENETRE_VISIO = Duration.ofMinutes(15);

    private final RendezVousRepository rendezVousRepository;
    private final RendezVousEvenements evenements;
    private final SallesVisio sallesVisio;

    public RendezVousReminderScheduler(RendezVousRepository rendezVousRepository,
                                       RendezVousEvenements evenements,
                                       SallesVisio sallesVisio) {
        this.rendezVousRepository = rendezVousRepository;
        this.evenements = evenements;
        this.sallesVisio = sallesVisio;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void sendReminders() {
        envoyerRappels(LocalDateTime.now());
    }

    void envoyerRappels(LocalDateTime maintenant) {
        List<RendezVous> prochains = rendezVousRepository.findByStatutAndDateHeureGreaterThanEqualAndDateHeureLessThan(
                "CONFIRME", maintenant, maintenant.plusHours(24).plusMinutes(1));
        for (RendezVous rdv : prochains) {
            Duration avant = Duration.between(maintenant, rdv.getDateHeure());
            if (estVisio(rdv) && avant.compareTo(FENETRE_VISIO) <= 0) {
                if (!envoye(rdv.getRappelVisioEnvoye())) {
                    assurerSalle(rdv);
                    evenements.rappelVisio(rdv, maintenant);
                    rdv.setRappelVisioEnvoye(true);
                    rdv.setRappelJourEnvoye(true);
                    rdv.setRappelVeilleEnvoye(true);
                    rendezVousRepository.save(rdv);
                }
            } else if (avant.compareTo(FENETRE_JOUR) <= 0) {
                if (!envoye(rdv.getRappelJourEnvoye())) {
                    evenements.rappelJour(rdv, maintenant);
                    rdv.setRappelJourEnvoye(true);
                    rdv.setRappelVeilleEnvoye(true);
                    rendezVousRepository.save(rdv);
                }
            } else if (avant.compareTo(DEBUT_VEILLE) >= 0 && !envoye(rdv.getRappelVeilleEnvoye())) {
                evenements.rappelVeille(rdv, maintenant);
                rdv.setRappelVeilleEnvoye(true);
                rendezVousRepository.save(rdv);
            }
        }
    }

    /**
     * Au démarrage : les visioconférences à venir réservées avant la création automatique des
     * salles en reçoivent une (sinon, ni le client ni le professionnel n'auraient de lien).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void completerSallesVisio() {
        List<RendezVous> sansSalle = rendezVousRepository.findByModeAndLienVisioIsNullAndStatutInAndDateHeureAfter(
                "VISIO", List.of("CONFIRME", "EN_ATTENTE"), LocalDateTime.now().minusHours(2));
        sansSalle.forEach(this::assurerSalle);
        if (!sansSalle.isEmpty()) {
            log.info("Salles de visioconférence créées pour {} rendez-vous à venir.", sansSalle.size());
        }
    }

    private void assurerSalle(RendezVous rdv) {
        if (rdv.getLienVisio() == null || rdv.getLienVisio().isBlank()) {
            rdv.setLienVisio(sallesVisio.nouvelleSalle());
            rendezVousRepository.save(rdv);
        }
    }

    private static boolean estVisio(RendezVous rdv) {
        return "VISIO".equalsIgnoreCase(rdv.getMode());
    }

    private static boolean envoye(Boolean rappel) {
        return Boolean.TRUE.equals(rappel);
    }
}
