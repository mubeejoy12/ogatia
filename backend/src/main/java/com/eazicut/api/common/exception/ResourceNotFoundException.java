package com.eazicut.api.common.exception;

/**
 * Thrown when a lookup by id / slug returns nothing. Handled by
 * {@link GlobalExceptionHandler} as HTTP 404 with an {@code ApiError}
 * body.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s not found: %s".formatted(resource, identifier));
    }
}
