package com.eazicut.api.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issue and parse HS256 JWT access tokens.
 *
 * <p><strong>Claims</strong> we mint:
 * <ul>
 *   <li>{@code sub}   — the user's UUID as a string.</li>
 *   <li>{@code email} — the display-cased email (convenience for
 *       {@code /auth/me} responses without a second DB lookup).</li>
 *   <li>{@code role}  — {@link Role} name ({@code CUSTOMER} or {@code ADMIN}).
 *       The filter (Stage 3) turns this into a {@code ROLE_*} authority.</li>
 *   <li>{@code iss}   — configured issuer ({@code eazicut.jwt.issuer}).</li>
 *   <li>{@code iat}   — issued-at.</li>
 *   <li>{@code exp}   — issued-at + {@code eazicut.jwt.access-token-ttl}.</li>
 * </ul>
 *
 * <p>The key is derived once at construction time. JJWT throws if the
 * key is shorter than 256 bits — that's the fail-fast we want in prod.
 *
 * <p>Rotation: change {@code EAZICUT_JWT_SECRET}, redeploy. Every
 * outstanding access token immediately fails signature validation; a
 * refresh-token flow (Stage 5) will re-issue seamlessly.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Mint an access token for a persisted user. Called by the login
     * flow (Stage 4) and by the refresh flow (Stage 5).
     */
    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(props.accessTokenTtl());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(props.issuer())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parse and validate a token. Throws {@link JwtException} on any
     * failure — expired, malformed, wrong signature, wrong issuer.
     * Callers catch it and translate to 401.
     */
    public ParsedAccessToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        Role role = Role.valueOf(claims.get("role", String.class));

        return new ParsedAccessToken(userId, email, role);
    }

    /**
     * Result of a successful parse — the fields the security filter
     * needs to build a Spring {@code Authentication}. Deliberately does
     * not carry the full {@link User} entity: verifying every request
     * against the DB would kill statelessness.
     */
    public record ParsedAccessToken(UUID userId, String email, Role role) {}
}
