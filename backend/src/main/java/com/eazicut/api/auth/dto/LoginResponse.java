package com.eazicut.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.eazicut.api.users.dto.UserResponse;

/**
 * Response from {@code POST /auth/login} and {@code POST /auth/refresh}.
 *
 * <p>Carries a short-lived access token plus the {@link UserResponse}
 * projection so the frontend can render the header ("Signed in as …")
 * without a second round-trip.
 *
 * <p>The {@code refreshToken} field is <strong>never serialised</strong>
 * ({@link JsonIgnore}) — the controller extracts it from this DTO,
 * attaches it as an {@code HttpOnly Secure SameSite=Lax} cookie, and
 * the JSON body sent to the browser omits it entirely. Keeping the
 * value on the DTO (rather than passing it as a separate return) makes
 * the {@code AuthService.login/refresh} contract cleaner — one
 * response type covers both.
 *
 * <p>The {@code expiresInSeconds} field is informational — the
 * authoritative expiry lives in the JWT's {@code exp} claim. The
 * frontend uses this value to schedule a silent refresh before the
 * access token dies.
 */
public record LoginResponse(
        String accessToken,
        long expiresInSeconds,
        UserResponse user,
        @JsonIgnore String refreshToken
) {
}
