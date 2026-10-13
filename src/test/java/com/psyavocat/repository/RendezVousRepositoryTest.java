package com.psyavocat.repository;

import com.psyavocat.entity.Client;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.RendezVous;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requêtes exécutées sur une vraie base (H2) : les tests à base de mocks ne
 * détectent pas une requête dérivée sur une propriété non mappée.
 */
@SpringBootTest
@Transactional
class RendezVousRepositoryTest {

    @Autowired private RendezVousRepository rendezVousRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    private Client client(String id) {
        Client c = new Client();
        c.setId(id);
        c.setNom("Traoré");
        c.setPrenom("Awa");
        c.setEmail(id + "@test.fr");
        return utilisateurRepository.save(c);
    }

    private Psychologue psychologue() {
        Psychologue p = new Psychologue();
        p.setId("psy-1");
        p.setNom("Diallo");
        p.setPrenom("Fanta");
        p.setEmail("psy-1@test.fr");
        p.setNumeroAgrement("AG-1");
        p.setStatutValidation("APPROVED");
        return utilisateurRepository.save(p);
    }

    private RendezVous rdv(Client client, Psychologue psy, LocalDateTime quand) {
        RendezVous r = new RendezVous();
        r.setClient(client);
        r.setProfessionnel(psy);
        r.setDateHeure(quand);
        r.setStatut("CONFIRME");
        r.setMode("VISIO");
        return rendezVousRepository.save(r);
    }

    @Test
    @DisplayName("Les rendez-vous d'un client se lisent (du plus récent au plus ancien)")
    void rendezVousDuClient() {
        Client awa = client("client-1");
        Client autre = client("client-2");
        Psychologue psy = psychologue();
        LocalDateTime demain = LocalDateTime.now().plusDays(1);
        rdv(awa, psy, demain);
        rdv(awa, psy, demain.plusDays(7));
        rdv(autre, psy, demain);

        List<RendezVous> liste = rendezVousRepository.findByClientIdOrderByDateHeureDesc("client-1");

        assertThat(liste).hasSize(2);
        assertThat(liste.get(0).getDateHeure()).isAfter(liste.get(1).getDateHeure());
        assertThat(rendezVousRepository.findByClientIdOrderByDateHeureDesc("inconnu")).isEmpty();
        assertThat(rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc("psy-1")).hasSize(3);
    }
}
