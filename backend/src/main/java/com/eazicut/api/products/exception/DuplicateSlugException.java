package com.eazicut.api.products.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to create or update a product with a slug that
 * already exists on another (non-deleted) product.
 */
public class DuplicateSlugException extends ConflictException {

    public DuplicateSlugException(String slug) {
        super("A product with slug '%s' already exists.".formatted(slug));
    }
}
