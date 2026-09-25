package com.psyavocat.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Token d'authentification Spring Security pour Firebase.
 */
public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;
    private final String credentials; // ID Token

    public FirebaseAuthenticationToken(
            AuthenticatedUser principal,
            String credentials,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
