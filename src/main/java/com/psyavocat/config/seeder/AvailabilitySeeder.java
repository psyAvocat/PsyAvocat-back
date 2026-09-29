package com.psyavocat.config.seeder;

import com.psyavocat.entity.Disponibilite;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.repository.AvocatRepository;
import com.psyavocat.repository.DisponibiliteRepository;
import com.psyavocat.repository.PsychologueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder dédié aux Disponibilités (créneaux horaires) des professionnels.
 * 100% Idempotent : Vérifie qu'aucun créneau n'est déjà initialisé pour chaque professionnel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvailabilitySeeder {

    private final AvocatRepository avocatRepository;
    private final PsychologueRepository psychologueRepository;
    private final DisponibiliteRepository disponibiliteRepository;

    @Transactional
    public void seed() {
        log.info("  [AvailabilitySeeder] Initialisation des créneaux de disponibilités (10 jours)...");

        List<Professionnel> professionnels = new ArrayList<>();
        professionnels.addAll(avocatRepository.findAll());
        professionnels.addAll(psychologueRepository.findAll());

        for (Professionnel pro : professionnels) {
            seedDisponibilitesForPro(pro);
        }
    }

    private void seedDisponibilitesForPro(Professionnel pro) {
        if (disponibiliteRepository.countByProfessionnelId(pro.getId()) > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<LocalTime> heures = List.of(
                LocalTime.of(9, 0),
                LocalTime.of(10, 30),
                LocalTime.of(14, 0),
                LocalTime.of(15, 30),
                LocalTime.of(17, 0),
                LocalTime.of(18, 30)
        );

        int count = 0;
        for (int day = 0; day < 10; day++) {
            LocalDate date = today.plusDays(day);
            for (LocalTime start : heures) {
                Disponibilite dispo = new Disponibilite();
                dispo.setDate(date);
                dispo.setHeureDebut(start);
                dispo.setHeureFin(start.plusMinutes(45));
                dispo.setStatut("DISPONIBLE");
                dispo.setProfessionnel(pro);
                disponibiliteRepository.save(dispo);
                count++;
            }
        }
        log.info("    + {} créneaux disponibles créés pour {} {}", count, pro.getPrenom(), pro.getNom());
    }
}
