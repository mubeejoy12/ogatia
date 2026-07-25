package com.eazicut.api.collections.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to delete a collection that still has products
 * referencing it.
 *
 * <p>Raised by {@code CollectionService.delete} <em>before</em> attempting
 * the DB delete so the caller gets a clear domain-level message
 * ("Cannot delete: 3 products still reference this collection.") rather
 * than a generic {@code DataIntegrityViolationException}.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class CollectionInUseException extends ConflictException {

    public CollectionInUseException(String identifier, long productCount) {
        super("Cannot delete collection '%s': %d product%s still reference%s it."
                .formatted(
                        identifier,
                        productCount,
                        productCount == 1 ? "" : "s",
                        productCount == 1 ? "s" : ""
                ));
    }
}
