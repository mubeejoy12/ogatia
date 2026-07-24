package com.eazicut.api.categories.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to delete a category that still has products
 * referencing it.
 *
 * <p>The service raises this <em>before</em> attempting the DB delete so
 * the caller gets a clear domain-level message ("Cannot delete: 3
 * products still reference this category.") rather than a generic
 * {@code DataIntegrityViolationException} that only says "conflict".
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class CategoryInUseException extends ConflictException {

    public CategoryInUseException(String identifier, long productCount) {
        super("Cannot delete category '%s': %d product%s still reference%s it."
                .formatted(
                        identifier,
                        productCount,
                        productCount == 1 ? "" : "s",
                        productCount == 1 ? "s" : ""
                ));
    }
}
