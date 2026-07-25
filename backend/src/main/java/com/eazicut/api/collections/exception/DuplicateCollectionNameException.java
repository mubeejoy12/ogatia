package com.eazicut.api.collections.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to create or update a collection with a name that
 * already exists on another collection (case-insensitive comparison —
 * "Onyx Bespoke" and "onyx bespoke" are the same collection).
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class DuplicateCollectionNameException extends ConflictException {

    public DuplicateCollectionNameException(String name) {
        super("A collection with the name '%s' already exists.".formatted(name));
    }
}
