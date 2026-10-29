package com.psyavocat.scheduler;

import com.psyavocat.entity.RendezVous;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.service.notification.RendezVousEvenements;
import com.psyavocat.service.support.SallesVisio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Rappels planifiés : bon rappel au bon moment, une seule fois chacun. */
class RendezVousReminderSchedulerTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 29, 9, 0);

    private final RendezVousRepository repository = mock(RendezVousRepository.class);
    private final RendezVousEvenements evenements = mock(RendezVousEvenements.class);
    private final RendezVousReminderScheduler scheduler =
            new RendezVousReminderScheduler(repository, evenements, new SallesVisio("https://visio.test"));
    private final List<RendezVous> prochains = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(repository.findByStatutAndDateHeureGreaterThanEqualAndDateHeureLessThan(eq("CONFIRME"), any(), any()))
                .thenReturn(prochains);
    }

    private RendezVous rdv(String mode, LocalDateTime debut) {
        RendezVous r = new RendezVous();
        r.setId(mode + "-" + debut);
        r.setMode(mode);
        r.setStatut("CONFIRME");
        r.setDateHeure(debut);
        prochains.add(r);
        return r;
    }

    @Test
    @DisplayName("Visio dans 10 min : rappel visio, une seule fois, salle créée si besoin")
    void visioImminente() {
        RendezVous visio = rdv("VISIO", MAINTENANT.plusMinutes(10));

        scheduler.envoyerRappels(MAINTENANT);
        scheduler.envoyerRappels(MAINTENANT.plusMinutes(5));

        verify(evenements, times(1)).rappelVisio(visio, MAINTENANT);
        verify(evenements, never()).rappelJour(any(), any());
        assertThat(visio.getLienVisio()).startsWith("https://visio.test/PsyAvocat-");
    }

    @Test
    @DisplayName("Dans 1 h 30 : rappel du jour, au cabinet comme en visio (le rappel visio viendra ensuite)")
    void rappelDuJour() {
        RendezVous cabinet = rdv("CABINET", MAINTENANT.plusMinutes(90));
        RendezVous visio = rdv("VISIO", MAINTENANT.plusMinutes(90));

        scheduler.envoyerRappels(MAINTENANT);

        verify(evenements).rappelJour(cabinet, MAINTENANT);
        verify(evenements).rappelJour(visio, MAINTENANT);
        verify(evenements, never()).rappelVisio(any(), any());

        // Un quart d'heure avant la visio : son rappel part, mais pas un second rappel du jour.
        scheduler.envoyerRappels(MAINTENANT.plusMinutes(80));
        verify(evenements).rappelVisio(visio, MAINTENANT.plusMinutes(80));
        verify(evenements, times(1)).rappelJour(cabinet, MAINTENANT);
    }

    @Test
    @DisplayName("Veille : entre 24 h et 20 h avant ; rien entre 20 h et 2 h avant")
    void rappelDeLaVeille() {
        RendezVous demain = rdv("CABINET", MAINTENANT.plusHours(22));
        RendezVous ceSoir = rdv("CABINET", MAINTENANT.plusHours(10));

        scheduler.envoyerRappels(MAINTENANT);
        scheduler.envoyerRappels(MAINTENANT.plusMinutes(5));

        verify(evenements, times(1)).rappelVeille(demain, MAINTENANT);
        verifyNoMoreInteractions(evenements);
        assertThat(ceSoir.getRappelVeilleEnvoye()).isNull();
    }

    @Test
    @DisplayName("Rendez-vous déplacé : ses rappels repartent pour la nouvelle date")
    void deplace_rappelsReinitialises() {
        RendezVous visio = rdv("VISIO", MAINTENANT.plusMinutes(10));
        scheduler.envoyerRappels(MAINTENANT);

        visio.reinitialiserRappels();
        scheduler.envoyerRappels(MAINTENANT.plusMinutes(1));

        verify(evenements, times(2)).rappelVisio(any(), any());
    }
}
