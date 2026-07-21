package com.eazicut.api.products.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to create or update a product with a SKU that
 * already exists on another (non-deleted) product.
 *
 * <p>Extends {@link ConflictException} so the global exception handler
 * translates it to HTTP 409 without any per-exception plumbing.
 */
public class DuplicateSkuException extends ConflictException {

    public DuplicateSkuException(String sku) {
        super("A product with SKU '%s' already exists.".formatted(sku));
    }
}
