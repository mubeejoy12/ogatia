package com.eazicut.api.categories.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when attempting to create or update a category with a slug that
 * already exists on another category.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class DuplicateCategorySlugException extends ConflictException {

    public DuplicateCategorySlugException(String slug) {
        super("A category with slug '%s' already exists.".formatted(slug));
    }
}
