package com.psyavocat.exception;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    /** Code fonctionnel facultatif, exploitable par les clients (ex. {@code PROFESSIONAL_NOT_APPROVED}). */
    private final String code;

    public ApiException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public ApiException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
