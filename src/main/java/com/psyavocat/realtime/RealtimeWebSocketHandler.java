package com.psyavocat.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Canal temps réel serveur → client. Le client n'envoie rien d'autre que des « ping »
 * de maintien de connexion : toute action métier passe par l'API REST.
 */
@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeGateway gateway;
    private final PresenceService presenceService;

    public RealtimeWebSocketHandler(RealtimeGateway gateway, PresenceService presenceService) {
        this.gateway = gateway;
        this.presenceService = presenceService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String uid = uidDe(session);
        if (gateway.enregistrer(uid, session)) {
            presenceService.signalerChangement(uid, true);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String uid = uidDe(session);
        if (gateway.retirer(uid, session)) {
            presenceService.signalerChangement(uid, false);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Maintien de connexion uniquement : aucun message client n'est interprété.
    }

    private String uidDe(WebSocketSession session) {
        return (String) session.getAttributes().get(RealtimeHandshakeInterceptor.ATTRIBUT_UID);
    }
}
