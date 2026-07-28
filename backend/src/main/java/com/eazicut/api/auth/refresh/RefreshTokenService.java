package com.eazicut.api.auth.refresh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.auth.exception.InvalidCredentialsException;
import com.eazicut.api.users.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Issue, rotate, and revoke refresh tokens.
 *
 * <p>The raw token is 256 bits of {@link SecureRandom} entropy, base64url-
 * encoded. Only the SHA-256 hash reaches the database; the raw value
 * lives only in the HttpOnly cookie the client stores. That way a DB
 * leak alone cannot compromise sessions.
 *
 * <p>Rotation model (call {@link #rotate}):
 * <ol>
 *   <li>Hash the incoming raw token.</li>
 *   <li>Look up the row; reject if missing, revoked, or expired.</li>
 *   <li>Mark the old row {@code revoked_at = now}.</li>
 *   <li>Issue a new random token for the same user, persist its hash.</li>
 *   <li>Return the (raw new token, user) pair to the caller — the
 *       controller uses the raw value to overwrite the cookie.</li>
 * </ol>
 *
 * <p>Reuse of a revoked token — a signal that a token was stolen and
 * replayed — is a topic for a later hardening pass. For launch we
 * respond 401 to the reuse attempt; a follow-up ticket can wire
 * "revoke the whole family on reuse" once refresh-token reuse
 * telemetry is available.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    /** Base64url alphabet ID length in bytes (32 → 256 bits of entropy). */
    private static final int TOKEN_ENTROPY_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    @Value("${eazicut.jwt.refresh-token-ttl:7d}")
    private Duration refreshTokenTtl;

    /**
     * Mint a fresh refresh token for the given user. Called by
     * {@code AuthService.login()} once credentials pass.
     *
     * @return the RAW token — persist it once (in the cookie) and
     *         forget it. Only the hash is retained server-side.
     */
    public IssuedToken issue(User user) {
        String raw = generateRawToken();
        RefreshToken row = new RefreshToken();
        row.setUser(user);
        row.setTokenHash(sha256(raw));
        row.setIssuedAt(Instant.now());
        row.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        repository.save(row);
        return new IssuedToken(raw, row.getExpiresAt());
    }

    /**
     * Consume the given raw refresh token and mint a new one for the
     * same user. Rotates atomically inside the surrounding transaction:
     * if minting the new row fails, the old row's revocation rolls back
     * and the caller can retry.
     *
     * @throws InvalidCredentialsException if the token is unknown,
     *         revoked, or expired — same generic error surface the
     *         login path uses, so the client can't distinguish causes.
     */
    public RotationResult rotate(String rawIncoming) {
        RefreshToken existing = repository.findByTokenHash(sha256(rawIncoming))
                .orElseThrow(InvalidCredentialsException::new);

        Instant now = Instant.now();
        if (!existing.isLive(now)) {
            throw new InvalidCredentialsException();
        }

        existing.setRevokedAt(now);
        User user = existing.getUser();
        if (!user.isEnabled()) {
            // Mid-session account disable — refuse before minting a
            // successor and force a fresh login (which will also fail
            // via AuthService.login's enabled check).
            throw new InvalidCredentialsException();
        }

        IssuedToken next = issue(user);
        return new RotationResult(user, next);
    }

    /**
     * Revoke the given raw refresh token if it exists and is still
     * live. Logout is deliberately idempotent — an unknown or already
     * -revoked token is a no-op, so double-logout doesn't error.
     */
    public void revoke(String rawIncoming) {
        if (rawIncoming == null || rawIncoming.isBlank()) return;
        repository.findByTokenHash(sha256(rawIncoming))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(Instant.now()));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_ENTROPY_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            // Hex, lowercase — deterministic 64 chars, matches the column length
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", e);
        }
    }

    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    /** The raw token to hand to the client + when it expires. */
    public record IssuedToken(String rawToken, Instant expiresAt) {}

    /** Result of a successful rotation. */
    public record RotationResult(User user, IssuedToken next) {}
}
