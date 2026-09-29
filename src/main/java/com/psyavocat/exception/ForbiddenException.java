package com.psyavocat.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
