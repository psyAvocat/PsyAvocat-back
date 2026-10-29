package com.psyavocat.realtime;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Client;
import com.psyavocat.entity.Conversation;
import com.psyavocat.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Statut « en ligne » : une connexion temps réel ouverte, sur n'importe quel appareil. */
class PresenceTest {

    private RealtimeGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new RealtimeGateway(JsonMapper.builder().build());
    }

    private static WebSocketSession session(String id) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    @DisplayName("En ligne dès la première connexion, hors ligne à la fermeture de la dernière")
    void plusieursAppareils() {
        WebSocketSession web = session("web");
        WebSocketSession mobile = session("mobile");

        assertThat(gateway.estEnLigne("u1")).isFalse();
        assertThat(gateway.enregistrer("u1", web)).isTrue();      // passage en ligne
        assertThat(gateway.enregistrer("u1", mobile)).isFalse();  // déjà en ligne
        assertThat(gateway.estEnLigne("u1")).isTrue();

        assertThat(gateway.retirer("u1", web)).isFalse();         // reste en ligne sur mobile
        assertThat(gateway.estEnLigne("u1")).isTrue();
        assertThat(gateway.retirer("u1", mobile)).isTrue();       // passage hors ligne
        assertThat(gateway.estEnLigne("u1")).isFalse();
    }

    @Test
    @DisplayName("Les correspondants sont prévenus du changement de présence")
    void correspondantsPrevenus() {
        Avocat avocat = new Avocat();
        avocat.setId("avocat-1");
        Client client = new Client();
        client.setId("client-1");
        Conversation conv = new Conversation();
        conv.setParticipants(List.of(avocat, client));

        ConversationRepository repo = mock(ConversationRepository.class);
        when(repo.findByParticipantId("client-1")).thenReturn(List.of(conv));
        RealtimeGateway gw = mock(RealtimeGateway.class);

        new PresenceService(repo, gw).signalerChangement("client-1", true);

        verify(gw).envoyerA(eq("avocat-1"), eq(PresenceService.EVENEMENT),
                eq(Map.of("utilisateurId", "client-1", "enLigne", true)));
        verify(gw, never()).envoyerA(eq("client-1"), any(), any());
    }
}
