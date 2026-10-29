package com.psyavocat.realtime;

import com.psyavocat.entity.Conversation;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Présence « en ligne » dans la messagerie.
 *
 * Un utilisateur est en ligne tant qu'il a au moins une connexion temps réel ouverte
 * (application web ou mobile). À chaque passage en ligne / hors ligne, ses
 * correspondants (participants de ses conversations) reçoivent un événement
 * {@code PRESENCE} : {@code {"utilisateurId": "...", "enLigne": true|false}}.
 */
@Slf4j
@Service
public class PresenceService {

    public static final String EVENEMENT = "PRESENCE";

    private final ConversationRepository conversationRepository;
    private final RealtimeGateway gateway;

    public PresenceService(ConversationRepository conversationRepository, RealtimeGateway gateway) {
        this.conversationRepository = conversationRepository;
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public void signalerChangement(String uid, boolean enLigne) {
        if (uid == null) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("utilisateurId", uid);
            payload.put("enLigne", enLigne);
            correspondantsDe(uid).forEach(id -> gateway.envoyerA(id, EVENEMENT, payload));
        } catch (Exception e) {
            // La présence est une information de confort : un échec ne doit jamais
            // empêcher l'ouverture ou la fermeture de la connexion.
            log.warn("Diffusion de la présence de {} impossible : {}", uid, e.getMessage());
        }
    }

    /** Toutes les personnes avec qui l'utilisateur a une conversation. */
    Set<String> correspondantsDe(String uid) {
        return conversationRepository.findByParticipantId(uid).stream()
                .map(Conversation::getParticipants)
                .filter(Objects::nonNull)
                .flatMap(participants -> participants.stream().map(Utilisateur::getId))
                .filter(id -> id != null && !id.equals(uid))
                .collect(Collectors.toSet());
    }
}
