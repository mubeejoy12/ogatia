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

    /**
     * Companion browser-visible presence cookie. Contains no credential
     * material — just the literal "1" — so it is safe to expose to
     * JavaScript and to Next.js middleware. Its sole job is to let the
     * frontend middleware answer "is this browser plausibly signed in?"
     * without needing to see the actual refresh token (which stays
     * HttpOnly at {@code Path=/api/v1/auth}).
     *
     * <p>Set alongside the real refresh cookie on login/refresh, cleared
     * alongside it on logout. If it's tampered with by the user (e.g.
     * hand-set in devtools), the worst outcome is a false-positive
     * middleware pass — the underlying /auth/me call still 401s and
     * the AuthContext then hard-redirects. Never trusted as authority.
     */
    public static final String SESSION_MARKER_NAME = "eazicut_session";
    public static final String SESSION_MARKER_PATH = "/";

    private RefreshCookies() {}

    /**
     * Attach two {@code Set-Cookie} headers: the HttpOnly refresh
     * cookie and the browser-visible session marker.
     */
    public static <T> ResponseEntity.BodyBuilder attachSet(
            ResponseEntity.BodyBuilder builder,
            String rawToken,
            Duration ttl
    ) {
        ResponseCookie refresh = ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(ttl)
                .build();
        ResponseCookie marker = ResponseCookie.from(SESSION_MARKER_NAME, "1")
                .httpOnly(false)   // frontend middleware and JS need to read it
                .secure(true)
                .sameSite("Lax")
                .path(SESSION_MARKER_PATH)
                .maxAge(ttl)
                .build();
        return builder
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.SET_COOKIE, marker.toString());
    }

    /**
     * Attach two {@code Set-Cookie} headers that clear both cookies.
     */
    public static <T> ResponseEntity.BodyBuilder attachClear(
            ResponseEntity.BodyBuilder builder
    ) {
        ResponseCookie refresh = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        ResponseCookie marker = ResponseCookie.from(SESSION_MARKER_NAME, "")
                .httpOnly(false)
                .secure(true)
                .sameSite("Lax")
                .path(SESSION_MARKER_PATH)
                .maxAge(0)
                .build();
        return builder
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.SET_COOKIE, marker.toString());
    }
}
