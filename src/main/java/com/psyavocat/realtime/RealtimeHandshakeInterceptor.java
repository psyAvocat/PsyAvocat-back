package com.psyavocat.realtime;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.service.support.ClientAccounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Authentifie l'ouverture d'un WebSocket avec le jeton Firebase (vérification
 * cryptographique stricte, comme pour l'API REST).
 *
 * Les clients WebSocket ne pouvant pas toujours envoyer d'en-tête Authorization,
 * le jeton est transmis en paramètre {@code token} de l'URL d'ouverture.
 */
@Slf4j
@Component
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    static final String ATTRIBUT_UID = "uid";

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final UtilisateurRepository utilisateurRepository;

    public RealtimeHandshakeInterceptor(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            UtilisateurRepository utilisateurRepository
    ) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (!StringUtils.hasText(token) || firebaseAuth == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            FirebaseToken decoded = firebaseAuth.verifyIdToken(token);
            Utilisateur utilisateur = utilisateurRepository.findById(decoded.getUid()).orElse(null);
            // Mêmes règles que l'API REST : compte désactivé ou client à l'e-mail non vérifié refusés.
            if (utilisateur == null || Boolean.FALSE.equals(utilisateur.getActif())
                    || (ClientAccounts.isClient(utilisateur) && !decoded.isEmailVerified())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            attributes.put(ATTRIBUT_UID, decoded.getUid());
            return true;
        } catch (Exception e) {
            log.debug("Ouverture WebSocket refusée : {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Rien à faire.
    }
}
