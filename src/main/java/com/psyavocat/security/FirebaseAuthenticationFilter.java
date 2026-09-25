package com.psyavocat.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.*;
import com.psyavocat.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public FirebaseAuthenticationFilter(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            UtilisateurRepository utilisateurRepository
    ) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
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

                    List<GrantedAuthority> authorities = determineAuthorities(utilisateur, decodedToken);

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
                    SecurityContextHolder.clearContext();
                    log.warn("Jeton Firebase ID invalide ou expiré : {}", e.getMessage());
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
    private List<GrantedAuthority> determineAuthorities(Utilisateur utilisateur, FirebaseToken token) {
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
        Object roleClaim = token.getClaims().get("role");
        if (roleClaim instanceof String roleStr && StringUtils.hasText(roleStr)) {
            String roleName = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr.toUpperCase();
            SimpleGrantedAuthority auth = new SimpleGrantedAuthority(roleName);
            if (!authorities.contains(auth)) {
                authorities.add(auth);
            }
        }

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }
}
