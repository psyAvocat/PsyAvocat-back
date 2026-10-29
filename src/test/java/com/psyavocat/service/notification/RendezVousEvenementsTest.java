package com.psyavocat.service.notification;

import com.psyavocat.entity.Client;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Rappels : chaque partie sait avec qui elle a rendez-vous, et la visio porte son lien. */
class RendezVousEvenementsTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final RendezVousEvenements evenements =
            new RendezVousEvenements(notificationService, mock(RealtimeGateway.class));

    private RendezVous visioA(LocalDateTime debut) {
        Client client = new Client();
        client.setId("client-1");
        client.setPrenom("Awa");
        client.setNom("Traoré");
        Psychologue psy = new Psychologue();
        psy.setId("psy-1");
        psy.setPrenom("Marc");
        psy.setNom("Dupont");
        RendezVous rdv = new RendezVous();
        rdv.setId("rdv-1");
        rdv.setMode("VISIO");
        rdv.setDateHeure(debut);
        rdv.setLienVisio("https://visio.test/PsyAvocat-abc");
        rdv.setPatient(client);
        rdv.setProfessionnel(psy);
        return rdv;
    }

    private List<NotificationEvenement> notifications() {
        ArgumentCaptor<NotificationEvenement> captor = ArgumentCaptor.forClass(NotificationEvenement.class);
        verify(notificationService, atLeastOnce()).notifier(captor.capture());
        return captor.getAllValues();
    }

    private NotificationEvenement pour(String destinataireId) {
        return notifications().stream().filter(n -> n.destinataireId().equals(destinataireId)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Visio imminente : le professionnel est prévenu avec le nom du client et le lien de la salle")
    void rappelVisio_professionnel() {
        LocalDateTime maintenant = LocalDateTime.of(2026, 10, 29, 10, 18);
        evenements.rappelVisio(visioA(LocalDateTime.of(2026, 10, 29, 10, 30)), maintenant);

        NotificationEvenement pro = pour("psy-1");
        assertThat(pro.type()).isEqualTo(RendezVousEvenements.RAPPEL_VISIO);
        assertThat(pro.titre()).isEqualTo("Visioconférence dans 12 min");
        assertThat(pro.message()).contains("Awa Traoré").contains("10h30");
        assertThat(pro.lienVisio()).isEqualTo("https://visio.test/PsyAvocat-abc");
        assertThat(pro.ressourceId()).isEqualTo("rdv-1");

        NotificationEvenement client = pour("client-1");
        assertThat(client.message()).contains("Dr Marc Dupont");
        assertThat(client.lienVisio()).isEqualTo("https://visio.test/PsyAvocat-abc");
    }

    @Test
    @DisplayName("Rappel de la veille : jour relatif et modalité, avec le nom de l'autre partie")
    void rappelVeille() {
        evenements.rappelVeille(visioA(LocalDateTime.of(2026, 10, 30, 10, 30)), LocalDateTime.of(2026, 10, 29, 12, 0));

        assertThat(pour("psy-1").message()).isEqualTo("Visioconférence avec Awa Traoré demain à 10h30.");
        assertThat(pour("client-1").message()).isEqualTo("Visioconférence avec Dr Marc Dupont demain à 10h30.");
    }
}
