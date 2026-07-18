package com.eazicut.api.common.exception;

/**
 * Thrown when a request is well-formed but semantically invalid
 * (business-rule violation, conflicting state, etc.). Handled by
 * {@link GlobalExceptionHandler} as HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
