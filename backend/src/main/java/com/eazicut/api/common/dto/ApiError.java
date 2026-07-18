package com.eazicut.api.common.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform error body — every non-2xx response from this API returns
 * an instance of this record.
 *
 * @param status    HTTP status code (e.g. 404, 400, 500)
 * @param error     short machine-readable error identifier (e.g. "not_found")
 * @param message   human-readable message safe to display to end users
 * @param path      the request path that produced the error
 * @param timestamp server-side timestamp
 * @param details   optional field-level validation errors, keyed by field name
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldViolation> details
) {

    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now(), null);
    }

    public static ApiError of(
            int status,
            String error,
            String message,
            String path,
            List<FieldViolation> details
    ) {
        return new ApiError(status, error, message, path, Instant.now(), details);
    }
}
