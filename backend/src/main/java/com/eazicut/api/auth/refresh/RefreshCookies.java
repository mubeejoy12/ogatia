package com.eazicut.api.auth.refresh;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

/**
 * Cookie shape for the refresh token — one central place so login,
 * refresh, and logout emit exactly the same attributes.
 *
 * <p><strong>Attributes.</strong>
 * <ul>
 *   <li>{@code HttpOnly} — JavaScript can't read it, so XSS can't
 *       exfiltrate it (D2).</li>
 *   <li>{@code Secure} — never sent over HTTP. Browsers still send it
 *       to {@code http://localhost} for dev, which is what we want.</li>
 *   <li>{@code SameSite=Lax} — with a bearer-only API and no state-
 *       changing GETs, Lax closes CSRF without adding a token dance.</li>
 *   <li>{@code Path=/api/v1/auth} — the cookie only rides on
 *       {@code /refresh} and {@code /logout} requests; not sent with
 *       every product/checkout call.</li>
 * </ul>
 *
 * <p>Cookie name {@code eazicut_refresh} — namespaced so browser dev
 * tools show which app owns it without conflict.
 */
public final class RefreshCookies {

    public static final String COOKIE_NAME = "eazicut_refresh";
    public static final String COOKIE_PATH = "/api/v1/auth";

    private RefreshCookies() {}

    /**
     * Attach a {@code Set-Cookie} header carrying the refresh token to
     * the given response builder.
     */
    public static <T> ResponseEntity.BodyBuilder attachSet(
            ResponseEntity.BodyBuilder builder,
            String rawToken,
            Duration ttl
    ) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(ttl)
                .build();
        return builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Attach a {@code Set-Cookie} header that clears the refresh
     * cookie — used by logout. Same attributes as attach so the
     * browser matches and deletes the old one; maxAge=0.
     */
    public static <T> ResponseEntity.BodyBuilder attachClear(
            ResponseEntity.BodyBuilder builder
    ) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        return builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
