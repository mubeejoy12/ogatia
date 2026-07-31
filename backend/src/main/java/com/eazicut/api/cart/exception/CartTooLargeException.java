package com.eazicut.api.cart.exception;

/**
 * Thrown when a cart add would push the number of distinct lines above
 * the per-cart cap (50).
 *
 * <p>Mapped by the global handler to HTTP 413 Payload Too Large — the
 * cart itself is the resource that's too large. Distinct from
 * {@link InsufficientStockException} (409, world-vs-request) — this is
 * a resource-limit issue.
 */
public class CartTooLargeException extends RuntimeException {

    public CartTooLargeException(int currentLines, int cap) {
        super("Cart has %d lines — the maximum is %d. Remove a piece before adding another."
                .formatted(currentLines, cap));
    }
}
