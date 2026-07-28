package com.eazicut.api.auth.controller;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.eazicut.api.auth.dto.LoginRequest;
import com.eazicut.api.auth.dto.LoginResponse;
import com.eazicut.api.auth.dto.RegisterRequest;
import com.eazicut.api.auth.refresh.RefreshCookies;
import com.eazicut.api.auth.service.AuthService;
import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.users.dto.UserResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for authentication.
 *
 * <p>Base path {@code /api/v1/auth}. Endpoints as of Stage 5:
 *
 * <ul>
 *   <li>{@code POST /auth/register} — public. Creates a CUSTOMER.</li>
 *   <li>{@code POST /auth/login}    — public. Verifies credentials,
 *       returns JWT access token + user, sets refresh cookie.</li>
 *   <li>{@code POST /auth/refresh}  — public (reads refresh cookie).
 *       Rotates the refresh token and mints a fresh access token.</li>
 *   <li>{@code POST /auth/logout}   — public. Revokes the refresh
 *       token and clears the cookie. Idempotent.</li>
 *   <li>{@code GET  /auth/me}       — authenticated. Returns the
 *       {@link UserResponse} projection for the caller.</li>
 * </ul>
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

    /**
     * Verify email + password, mint an access token, set the refresh
     * cookie. The refresh token itself never appears in the JSON body
     * ({@link LoginResponse#refreshToken} is {@code @JsonIgnore}) —
     * only the cookie carries it.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        LoginResponse body = authService.login(request, servletRequest.getRemoteAddr());
        var builder = ResponseEntity.ok();
        RefreshCookies.attachSet(builder, body.refreshToken(), authService.refreshTokenTtl());
        return builder.body(ApiResponse.of(body));
    }

    /**
     * Rotate the refresh token from the cookie. Returns the same
     * {@link LoginResponse} shape as {@link #login} so the frontend
     * has one code path.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = RefreshCookies.COOKIE_NAME, required = false) String refreshCookie
    ) {
        LoginResponse body = authService.refresh(refreshCookie);
        var builder = ResponseEntity.ok();
        RefreshCookies.attachSet(builder, body.refreshToken(), authService.refreshTokenTtl());
        return builder.body(ApiResponse.of(body));
    }

    /**
     * Revoke the refresh token (if present) and clear the cookie.
     * Always returns 204, whether or not there was a live session —
     * idempotent by design.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookies.COOKIE_NAME, required = false) String refreshCookie
    ) {
        authService.logout(refreshCookie);
        var builder = ResponseEntity.status(HttpStatus.NO_CONTENT);
        RefreshCookies.attachClear(builder);
        return builder.build();
    }

    /**
     * Return the current authenticated user.
     *
     * <p>Requires a valid Bearer access token. The
     * {@code JwtAuthenticationFilter} populates the SecurityContext's
     * principal with the email from the token's {@code email} claim;
     * we surface it here via {@link AuthenticationPrincipal} and hand
     * it to the service, which re-hydrates the row.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal String email) {
        return ApiResponse.of(authService.me(email));
    }
}
