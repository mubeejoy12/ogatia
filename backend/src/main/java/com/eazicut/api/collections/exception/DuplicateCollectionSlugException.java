package com.eazicut.api.collections.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to create or update a collection with a slug that
 * already exists on another collection.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class DuplicateCollectionSlugException extends ConflictException {

    public DuplicateCollectionSlugException(String slug) {
        super("A collection with slug '%s' already exists.".formatted(slug));
    }
}
