package com.eazicut.api.auth.refresh;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link RefreshToken}.
 *
 * <p>Only lookups by the pre-computed hash are exposed — no {@code findByUser}
 * / list surface. That's deliberate: /refresh takes the incoming cookie
 * value, hashes it, and looks up the exact row. Multi-session listing
 * (e.g. an admin "sign out everywhere" surface) is a later ticket.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
