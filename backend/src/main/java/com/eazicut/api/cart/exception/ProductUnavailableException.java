package com.eazicut.api.cart.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when a cart add references a product that is not currently
 * purchasable — soft-deleted, {@code DRAFT}, {@code INACTIVE},
 * {@code OUT_OF_STOCK}, or {@code ARCHIVED}.
 *
 * <p>Distinct from {@link com.eazicut.api.common.exception.ResourceNotFoundException}:
 * the product exists, but its lifecycle state forbids new lines.
 * Maps to HTTP 409.
 */
public class ProductUnavailableException extends ConflictException {

    public ProductUnavailableException(String productSlug) {
        super("Product '%s' is not currently available for purchase.".formatted(productSlug));
    }
}
