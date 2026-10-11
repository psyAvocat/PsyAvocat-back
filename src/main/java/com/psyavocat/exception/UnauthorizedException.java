package com.psyavocat.exception;

import org.springframework.http.HttpStatus;

/**
 * Requête sans utilisateur authentifié (jeton absent, invalide ou expiré) : 401.
 */
public class UnauthorizedException extends ApiException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
