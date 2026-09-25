package com.psyavocat.security;

import com.psyavocat.entity.Utilisateur;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Représentation de l'utilisateur authentifié dans le SecurityContext de Spring Security.
 * Contient le Firebase UID, l'e-mail vérifié, l'entité métier MySQL (si existante)
 * ainsi que les rôles attribués.
 */
@Getter
public class AuthenticatedUser implements Serializable {

    private final String firebaseUid;
    private final String email;
    private final Utilisateur utilisateur;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(
            String firebaseUid,
            String email,
            Utilisateur utilisateur,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.utilisateur = utilisateur;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    public boolean hasMetierProfile() {
        return utilisateur != null;
    }

    public String getNom() {
        return utilisateur != null ? utilisateur.getNom() : null;
    }

    public String getPrenom() {
        return utilisateur != null ? utilisateur.getPrenom() : null;
    }
}
