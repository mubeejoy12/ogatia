package com.eazicut.api.cart.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when a cart add references a size the product does not offer.
 *
 * <p>Different from {@link ProductUnavailableException} (which is
 * lifecycle-level) — the product is fine, the specific size choice
 * isn't stocked. HTTP 409.
 */
public class SizeUnavailableException extends ConflictException {

    public SizeUnavailableException(String productSlug, String size) {
        super("Size '%s' is not offered for product '%s'.".formatted(size, productSlug));
    }
}
