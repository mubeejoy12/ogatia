package com.eazicut.api.auth.refresh;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.eazicut.api.users.entity.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent record of an issued refresh token.
 *
 * <p>The raw token value is never stored — only its SHA-256 hash. The
 * raw value lives only in the HttpOnly {@code eazicut_refresh} cookie
 * on the browser. Lookup on /refresh hashes the incoming cookie value
 * and matches against {@code token_hash}.
 *
 * <p>Deliberately NOT auditable via {@link com.eazicut.api.common.entity.AbstractAuditableEntity}
 * — this table already carries {@code issued_at} and {@code expires_at}
 * as domain-meaningful timestamps, and {@code created_at}/{@code updated_at}
 * on top would just be noise.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_user", columnList = "user_id")
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * The user this token belongs to. Lazy — /refresh only needs the
     * user for the enabled-flag re-check and for minting the next
     * access token, both of which happen server-side.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Tombstone. Null when live; set to the revocation instant on
     * rotation (during /refresh) or explicit logout.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isLive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
