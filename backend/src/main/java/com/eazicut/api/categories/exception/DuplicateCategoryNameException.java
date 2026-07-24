package com.eazicut.api.categories.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to create or update a category with a name that
 * already exists on another category (case-insensitive comparison —
 * "Suits" and "suits" are the same category).
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class DuplicateCategoryNameException extends ConflictException {

    public DuplicateCategoryNameException(String name) {
        super("A category with the name '%s' already exists.".formatted(name));
    }
}
