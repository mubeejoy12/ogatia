package com.eazicut.api.users.dto;

import java.time.Instant;
import java.util.UUID;

import com.eazicut.api.users.entity.Role;

/**
 * Wire shape for a {@code User} — the payload returned by registration
 * (Stage 2) and later by {@code /auth/me} (Stage 6).
 *
 * <p>Never carries {@code passwordHash}, never carries {@code emailLower}
 * (implementation detail — the client only needs the display casing),
 * never carries the {@code enabled} flag (also implementation detail —
 * a disabled user simply can't authenticate).
 */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        Role role,
        Instant createdAt
) {
}
