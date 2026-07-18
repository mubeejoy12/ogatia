package com.eazicut.api.common.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform success envelope for API responses.
 *
 * <p>Feature controllers should return either the raw payload or wrap it
 * in this record when a consistent success envelope is desired. The
 * frontend can rely on the presence of {@code data} to distinguish a
 * success payload from an {@link ApiError}.
 *
 * @param data       the response payload
 * @param timestamp  server-side timestamp of the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, Instant timestamp) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Instant.now());
    }
}
