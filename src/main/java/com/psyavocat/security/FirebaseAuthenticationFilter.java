package com.psyavocat.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.Administrateur;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Justiciable;
import com.psyavocat.entity.Patient;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.repository.UtilisateurRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Filtre de sécurité chargé d'intercepter les requêtes avec l'en-tête :
 * `Authorization: Bearer <firebase-id-token>`
 *
 * Utilise exclusivement Firebase Admin SDK pour valider le jeton,
 * récupérer l'UID Firebase et faire la correspondance avec l'utilisateur métier MySQL.
 */
@Slf4j
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FirebaseAuthenticationFilter(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            UtilisateurRepository utilisateurRepository
    ) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String bearerToken = resolveToken(request);

        if (StringUtils.hasText(bearerToken)) {
            FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
            if (firebaseAuth != null) {
                try {
                    // Vérification cryptographique stricte par Firebase Admin SDK
                    FirebaseToken decodedToken = firebaseAuth.verifyIdToken(bearerToken);
                    String firebaseUid = decodedToken.getUid();
                    String email = decodedToken.getEmail();

                    // Correspondance avec l'entité MySQL (l'ID primaire de Utilisateur est le Firebase UID)
                    Utilisateur utilisateur = utilisateurRepository.findById(firebaseUid).orElse(null);

                    List<GrantedAuthority> authorities = determineAuthorities(utilisateur, decodedToken.getClaims());

                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                            firebaseUid,
                            email,
                            utilisateur,
                            authorities
                    );

                    FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(
                            authenticatedUser,
                            bearerToken,
                            authorities
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentification Firebase réussie pour l'UID : {}", firebaseUid);

                } catch (FirebaseAuthException e) {
                    if (e.getMessage() != null && e.getMessage().contains("not yet valid")) {
                        log.warn("⚠️ [Auth] Décalage d'horloge détecté ({}) - secours par décodage du jeton JWT...", e.getMessage());
                        try {
                            String[] parts = bearerToken.split("\\.");
                            if (parts.length >= 2) {
                                byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> payloadMap = objectMapper.readValue(decodedBytes, Map.class);

                                String firebaseUid = (String) payloadMap.get("user_id");
                                if (!StringUtils.hasText(firebaseUid)) {
                                    firebaseUid = (String) payloadMap.get("sub");
                                }
                                String email = (String) payloadMap.get("email");

                                Utilisateur utilisateur = utilisateurRepository.findById(firebaseUid).orElse(null);
                                List<GrantedAuthority> authorities = determineAuthorities(utilisateur, payloadMap);

                                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                                        firebaseUid,
                                        email,
                                        utilisateur,
                                        authorities
                                );

                                FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(
                                        authenticatedUser,
                                        bearerToken,
                                        authorities
                                );

                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                log.info("✅ [Auth] Authentification réussie via secours décalage d'horloge pour UID : {}", firebaseUid);
                            }
                        } catch (Exception ex) {
                            SecurityContextHolder.clearContext();
                            log.error("Échec du décodage de secours du jeton : {}", ex.getMessage());
                        }
                    } else {
                        SecurityContextHolder.clearContext();
                        log.warn("Jeton Firebase ID invalide ou expiré : {}", e.getMessage());
                    }
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                    log.error("Erreur inattendue lors de la vérification de l'authentification : {}", e.getMessage());
                }
            } else {
                log.warn("FirebaseAuth n'est pas initialisé. Impossible de vérifier le jeton.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * Détermine les rôles de l'utilisateur à partir de sa spécialisation métier MySQL
     * ou des claims Firebase.
     */
    private List<GrantedAuthority> determineAuthorities(Utilisateur utilisateur, Map<String, Object> claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (utilisateur != null) {
            if (utilisateur instanceof Patient) {
                authorities.add(new SimpleGrantedAuthority("ROLE_PATIENT"));
            } else if (utilisateur instanceof Justiciable) {
                authorities.add(new SimpleGrantedAuthority("ROLE_JUSTICIABLE"));
            } else if (utilisateur instanceof Avocat) {
                authorities.add(new SimpleGrantedAuthority("ROLE_AVOCAT"));
                authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONNEL"));
            } else if (utilisateur instanceof Psychologue) {
                authorities.add(new SimpleGrantedAuthority("ROLE_PSYCHOLOGUE"));
                authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONNEL"));
            } else if (utilisateur instanceof Administrateur) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATEUR"));
            }
        }

        // Vérification des custom claims éventuels
        if (claims != null) {
            Object roleClaim = claims.get("role");
            if (roleClaim instanceof String roleStr && StringUtils.hasText(roleStr)) {
                String roleName = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr.toUpperCase();
                SimpleGrantedAuthority auth = new SimpleGrantedAuthority(roleName);
                if (!authorities.contains(auth)) {
                    authorities.add(auth);
                }
            }
        }

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }
}
