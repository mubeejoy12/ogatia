package com.eazicut.api.common.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.eazicut.api.common.dto.ApiError;
import com.eazicut.api.common.dto.ApiError.FieldViolation;

/**
 * Central exception handler. Every non-2xx response from this API
 * flows through here so the client sees a consistent {@link ApiError}
 * body — never a stack trace, never framework internals.
 *
 * <p>The handler order matters: the more specific handler wins over
 * the generic {@link #handleUnknown} fallback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), request);
    }

    /**
     * Domain-level conflicts — duplicate slug / SKU, invariant violations.
     * Feature exceptions extend {@link ConflictException} so this one handler
     * covers every case without per-exception plumbing.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "conflict", ex.getMessage(), request);
    }

    /** Bean-validation errors on {@code @Valid @RequestBody} payloads. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "validation_failed",
                "One or more fields failed validation.",
                request.getRequestURI(),
                violations
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "route_not_found", "No handler for " + ex.getRequestURL(), request);
    }

    /**
     * DB integrity constraint violation — unique keys, foreign keys, NOT NULL.
     * Almost always a race between the service's uniqueness probe and the DB
     * (or a bad FK reference); either way it is a semantic conflict, not a
     * server bug, so we translate to 409.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(
                HttpStatus.CONFLICT,
                "conflict",
                "The request conflicts with existing data.",
                request
        );
    }

    /**
     * Bad sort property in a Pageable — e.g. {@code ?sort=nonexistent,asc}.
     * Spring Data throws PropertyReferenceException; surface it as a 400 so
     * clients see an actionable error rather than a 500.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleBadSort(
            PropertyReferenceException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "invalid_sort",
                "Unknown sort property: '%s'.".formatted(ex.getPropertyName()),
                request
        );
    }

    /**
     * Path or query parameter can't be converted to the target type — e.g. a
     * non-UUID passed to {@code GET /products/{id}}, or a garbage value for
     * an enum query param.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type";
        return build(
                HttpStatus.BAD_REQUEST,
                "invalid_parameter",
                "Parameter '%s' must be a valid %s.".formatted(ex.getName(), expected),
                request
        );
    }

    /**
     * Missing / invalid credentials on an endpoint that requires them.
     * Distinct from {@link AccessDeniedException} — the caller isn't
     * <em>authenticated at all</em>, so the correct answer is 401 (client
     * should present credentials), not 403 (credentials rejected).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.UNAUTHORIZED,
                "unauthenticated",
                "Authentication is required to access this resource.",
                request
        );
    }

    /**
     * Authenticated principal lacks the authority the endpoint requires
     * (e.g. {@code @PreAuthorize("hasRole('ADMIN')")} on a POST while the
     * caller is anonymous or a non-admin user).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.FORBIDDEN,
                "forbidden",
                "You do not have permission to perform this action.",
                request
        );
    }

    /**
     * Fallback — logs the full exception server-side, returns a generic
     * 500 to the client with no internals leaked.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "The atelier is unavailable for a moment. Please try again.",
                request
        );
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        ApiError body = ApiError.of(status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
