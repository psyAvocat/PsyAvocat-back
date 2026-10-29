package com.psyavocat.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Point d'envoi unique des événements temps réel (WebSocket) vers les clients connectés.
 *
 * Format d'un message : {@code {"type": "...", "payload": {...}}}.
 * Un utilisateur peut être connecté depuis plusieurs appareils.
 */
@Slf4j
@Component
public class RealtimeGateway {

    /** Taille / délai max d'envoi par session (protège contre un client lent). */
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Map<String, Set<WebSocketSession>> sessionsParUtilisateur = new ConcurrentHashMap<>();
    private final JsonMapper jsonMapper;

    public RealtimeGateway(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Ajoute une session ouverte.
     * @return {@code true} si c'est la première session de l'utilisateur (il vient de passer en ligne)
     */
    boolean enregistrer(String uid, WebSocketSession session) {
        WebSocketSession sure = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        boolean[] premiere = {false};
        sessionsParUtilisateur.compute(uid, (cle, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
            }
            premiere[0] = sessions.isEmpty();
            sessions.add(sure);
            return sessions;
        });
        return premiere[0];
    }

    /**
     * Retire une session fermée.
     * @return {@code true} si c'était la dernière session de l'utilisateur (il vient de passer hors ligne)
     */
    boolean retirer(String uid, WebSocketSession session) {
        boolean[] derniere = {false};
        sessionsParUtilisateur.computeIfPresent(uid, (cle, sessions) -> {
            sessions.removeIf(s -> s.getId().equals(session.getId()));
            derniere[0] = sessions.isEmpty();
            return sessions.isEmpty() ? null : sessions;
        });
        return derniere[0];
    }

    /** Vrai si l'utilisateur a au moins une connexion temps réel ouverte (web ou mobile). */
    public boolean estEnLigne(String uid) {
        Set<WebSocketSession> sessions = uid != null ? sessionsParUtilisateur.get(uid) : null;
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /** Envoie un événement à toutes les sessions ouvertes d'un utilisateur. */
    public void envoyerA(String uid, String type, Object payload) {
        Set<WebSocketSession> sessions = sessionsParUtilisateur.get(uid);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = serialiser(type, payload);
        if (message != null) {
            sessions.forEach(session -> envoyer(session, message));
        }
    }

    /**
     * Diffuse un événement public (sans donnée personnelle) à tous les clients connectés,
     * par exemple « un créneau a changé » ou « un contenu a été publié ».
     */
    public void diffuser(String type, Object payload) {
        TextMessage message = serialiser(type, payload);
        if (message != null) {
            sessionsParUtilisateur.values().forEach(sessions -> sessions.forEach(s -> envoyer(s, message)));
        }
    }

    private TextMessage serialiser(String type, Object payload) {
        try {
            Map<String, Object> enveloppe = new LinkedHashMap<>();
            enveloppe.put("type", type);
            enveloppe.put("payload", payload);
            return new TextMessage(jsonMapper.writeValueAsString(enveloppe));
        } catch (Exception e) {
            log.error("Sérialisation de l'événement temps réel {} impossible : {}", type, e.getMessage());
            return null;
        }
    }

    private void envoyer(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(message);
        } catch (Exception e) {
            log.debug("Envoi WebSocket impossible sur la session {} : {}", session.getId(), e.getMessage());
        }
    }
}
