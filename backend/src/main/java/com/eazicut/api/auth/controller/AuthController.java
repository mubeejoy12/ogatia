package com.eazicut.api.auth.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.eazicut.api.auth.dto.RegisterRequest;
import com.eazicut.api.auth.service.AuthService;
import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.users.dto.UserResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for authentication (Stage 2 — registration only).
 *
 * <p>Base path {@code /api/v1/auth}. Login, refresh, logout, and /me
 * arrive in Stages 4–6. The endpoints all live under one controller so
 * the auth surface stays visible in one place.
 *
 * <p><strong>Access.</strong> {@code /auth/register} is public — every
 * new customer starts unauthenticated. When Stage 3 flips the filter
 * chain from {@code permitAll} to {@code authenticated}, this path must
 * be on the public allowlist alongside {@code /auth/login} and
 * {@code /auth/refresh}.
 *
 * <p><strong>Response shape.</strong> 201 Created with a {@code Location}
 * header pointing at the {@code /auth/me} URL the created user will use
 * once /me exists (Stage 6). Body carries the {@code UserResponse} in
 * the standard {@link ApiResponse} envelope.
 *
 * <p><strong>Validation.</strong> {@code @Valid} on the body means
 * missing fields, bad email shape, an out-of-range password, or a
 * blocklisted password all surface as HTTP 400 with the uniform
 * {@code validation_failed} shape from {@code GlobalExceptionHandler}.
 * Duplicate-email surfaces as HTTP 409 via
 * {@code DuplicateEmailException}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse created = authService.register(request);
        URI meLocation = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/auth/me")
                .build()
                .toUri();
        return ResponseEntity.created(meLocation).body(ApiResponse.of(created));
    }
}
