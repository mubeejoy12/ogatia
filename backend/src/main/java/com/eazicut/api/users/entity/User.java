package com.eazicut.api.users.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.eazicut.api.common.entity.AbstractAuditableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Application user — the identity behind every authenticated request.
 *
 * <p>Not a Spring Security {@code UserDetails} on purpose: the entity is a
 * domain object, adapted into {@code UserDetails} at the security boundary
 * by {@code UserDetailsAdapter} (Stage 1). Keeping the entity framework-free
 * means the domain doesn't leak Spring types into the service layer.
 *
 * <p><strong>Email normalisation.</strong> The {@link #syncEmailLower()}
 * lifecycle callback populates {@code emailLower} from {@link #email}
 * (trimmed + lowercased) on every persist / update. Consumers store the
 * caller-supplied casing in {@code email} for display; uniqueness and
 * lookups go through {@code emailLower}, backed by the
 * {@code ux_user_email_lower} unique index (V4 migration). Same
 * two-layer pattern V2/V3 use for Category/Collection names.
 *
 * <p><strong>Password.</strong> Only the BCrypt hash is stored, never the
 * raw password. Column width 72 matches BCrypt's Modular Crypt Format
 * output; a future upgrade to Argon2 needs a widening migration.
 *
 * <p><strong>Not soft-deleted.</strong> Auth records are compliance-
 * sensitive; account removal is either a hard delete (GDPR-style) or a
 * disable via {@link #enabled}. A soft-delete tombstone would complicate
 * "was this email ever registered" without giving a clear win.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_role", columnList = "role")
        }
)
public class User extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    /**
     * Lower-cased projection of {@link #email}, maintained by
     * {@link #syncEmailLower()}. Backs the {@code ux_user_email_lower}
     * unique index — see V4 migration for the portability rationale.
     */
    @Column(name = "email_lower", nullable = false, length = 255)
    private String emailLower;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @PrePersist
    @PreUpdate
    private void syncEmailLower() {
        this.emailLower = email == null ? null : email.trim().toLowerCase();
    }
}
