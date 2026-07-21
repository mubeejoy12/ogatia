package com.eazicut.api.common.exception;

/**
 * Base type for domain conflicts — a request that is well-formed but
 * cannot be satisfied because it would violate a uniqueness or state
 * invariant (duplicate slug, duplicate SKU, attempting to fulfil an
 * already-shipped order, …).
 *
 * <p>Handled by {@link GlobalExceptionHandler} as HTTP 409 with an
 * {@code ApiError} body. Feature-specific conflicts extend this class
 * so the handler stays feature-agnostic and every conflict returns the
 * same shape.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
