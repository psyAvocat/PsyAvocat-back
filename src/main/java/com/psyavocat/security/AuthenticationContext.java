package com.psyavocat.security;

import com.psyavocat.entity.Utilisateur;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Service d'accès au contexte d'authentification utilisateur.
 * Permet aux services métier d'accéder à l'utilisateur authentifié
 * sans dépendre directement de SecurityContextHolder.
 */
@Component
public class AuthenticationContext {

    /**
     * Récupère l'utilisateur authentifié courant s'il existe.
     */
    public Optional<AuthenticatedUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Récupère le Firebase UID de l'utilisateur connecté ou lève une exception.
     */
    public String getRequiredFirebaseUid() {
        return getCurrentUser()
                .map(AuthenticatedUser::getFirebaseUid)
                .orElseThrow(() -> new IllegalStateException("Aucun utilisateur authentifié dans le contexte de sécurité"));
    }

    /**
     * Récupère l'entité métier MySQL de l'utilisateur connecté s'il est enregistré.
     */
    public Optional<Utilisateur> getCurrentUtilisateur() {
        return getCurrentUser().map(AuthenticatedUser::getUtilisateur);
    }

    /**
     * Vérifie si la requête actuelle est effectuée par un utilisateur authentifié.
     */
    public boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    /**
     * Récupère les rôles / autorités de l'utilisateur connecté.
     */
    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getCurrentUser()
                .map(AuthenticatedUser::getAuthorities)
                .orElse(Collections.emptyList());
    }
}
