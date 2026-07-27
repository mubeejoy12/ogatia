package com.eazicut.api.auth.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bindable configuration for JWT issuance + validation.
 *
 * <p>Bound from {@code eazicut.jwt.*} — YAML in dev, env vars
 * ({@code EAZICUT_JWT_SECRET}, {@code EAZICUT_JWT_ACCESS_TOKEN_TTL}) in
 * prod. The secret must be at least 256 bits for HS256; JJWT enforces
 * this at key-construction time and will throw on boot with a shorter
 * one — a fail-fast we want.
 *
 * @param secret          base64-or-plain HS256 signing key (≥ 32 bytes)
 * @param issuer          value written to the {@code iss} claim; identifies our API
 * @param accessTokenTtl  lifetime of an issued access token (Stage 3 default 15 min)
 */
@ConfigurationProperties(prefix = "eazicut.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl
) {
}
