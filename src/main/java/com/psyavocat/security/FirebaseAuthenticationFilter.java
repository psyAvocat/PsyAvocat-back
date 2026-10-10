package com.psyavocat.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.Administrateur;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Client;
import com.psyavocat.entity.Justiciable;
import com.psyavocat.entity.Patient;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.service.support.ClientAccounts;
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
import java.util.List;

/**
 * Filtre de sécurité chargé d'intercepter les requêtes avec l'en-tête :
 * `Authorization: Bearer <firebase-id-token>`
 *
 * Utilise exclusivement Firebase Admin SDK pour valider le jeton,
 * récupérer l'UID Firebase et faire la correspondance avec l'utilisateur métier MySQL.
 * Les rôles proviennent UNIQUEMENT du profil métier MySQL (aucun custom claim Firebase).
 *
 * Refus de compte (403 JSON avec un {@code code} exploitable par les clients) :
 * - {@value #CODE_ACCOUNT_DISABLED} : compte désactivé par l'administration ;
 * - {@value #CODE_EMAIL_NOT_VERIFIED} : client dont l'adresse e-mail n'est pas vérifiée.
 * Seul {@code GET /me} reste accessible, pour que le client affiche la raison du refus.
 */
@Slf4j
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    public static final String CODE_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String CODE_EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SESSION_PATH = "/me";

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final UtilisateurRepository utilisateurRepository;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public FirebaseAuthenticationFilter(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            UtilisateurRepository utilisateurRepository,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.utilisateurRepository = utilisateurRepository;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String bearerToken = resolveToken(request);

        if (StringUtils.hasText(bearerToken)) {
            SecurityContextHolder.clearContext();
            FirebaseToken decodedToken = verify(bearerToken);

            if (decodedToken != null) {
                String firebaseUid = decodedToken.getUid();
                Utilisateur utilisateur = utilisateurRepository.findById(firebaseUid).orElse(null);
                boolean emailVerified = decodedToken.isEmailVerified();

                if (utilisateur != null && !isSessionEndpoint(request)) {
                    if (Boolean.FALSE.equals(utilisateur.getActif())) {
                        accessDeniedHandler.writeForbidden(request, response, CODE_ACCOUNT_DISABLED,
                                "Votre compte a été désactivé. Contactez le support PsyAvocat.");
                        return;
                    }
                    if (ClientAccounts.isClient(utilisateur) && !emailVerified) {
                        accessDeniedHandler.writeForbidden(request, response, CODE_EMAIL_NOT_VERIFIED,
                                "Veuillez confirmer votre adresse e-mail pour accéder à PsyAvocat.");
                        return;
                    }
                }

                List<GrantedAuthority> authorities = determineAuthorities(utilisateur);
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                        firebaseUid,
                        decodedToken.getEmail(),
                        emailVerified,
                        utilisateur,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(
                        new FirebaseAuthenticationToken(authenticatedUser, bearerToken, authorities)
                );
                log.debug("Authentification Firebase réussie pour l'UID : {}", firebaseUid);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Vérification cryptographique stricte par Firebase Admin SDK.
     * Échec sûr : un jeton non vérifié n'authentifie JAMAIS (retour {@code null}).
     */
    private FirebaseToken verify(String bearerToken) {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            log.error("[Auth] Firebase Admin indisponible : aucune requête ne peut être authentifiée.");
            return null;
        }
        try {
            return firebaseAuth.verifyIdToken(bearerToken);
        } catch (Exception e) {
            log.warn("[Auth] Jeton Firebase refusé : {}", e.getMessage());
            return null;
        }
    }

    private boolean isSessionEndpoint(HttpServletRequest request) {
        return (request.getContextPath() + SESSION_PATH).equals(request.getRequestURI());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * Détermine les rôles de l'utilisateur à partir de sa spécialisation métier MySQL.
     */
    private List<GrantedAuthority> determineAuthorities(Utilisateur utilisateur) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (utilisateur instanceof Client) {
            // Profil client unifié : valable dans l'univers Psychologue ET Avocat.
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
            authorities.add(new SimpleGrantedAuthority("ROLE_PATIENT"));
            authorities.add(new SimpleGrantedAuthority("ROLE_JUSTICIABLE"));
        } else if (utilisateur instanceof Patient) {
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

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }
}
