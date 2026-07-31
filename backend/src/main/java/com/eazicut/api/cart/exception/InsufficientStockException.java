package com.eazicut.api.cart.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when an add or patch would push a line's quantity above the
 * product's currently available stock.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409 — a semantic
 * conflict between the request and the current world, not a
 * validation error (the request was well-formed; the world moved).
 * The message includes the available count so the frontend can render
 * "only 3 left" copy.
 */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(String productSlug, int requested, int available) {
        super("Only %d of '%s' available (requested %d).".formatted(available, productSlug, requested));
    }
}
