package com.eazicut.api.users.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.users.entity.User;

/**
 * Spring Data repository for {@link User}.
 *
 * <p>All lookups by email go through the case-insensitive derived query
 * {@link #findByEmailLower(String)} — the caller passes the normalised
 * (trim + lowercase) form and the DB uses {@code ux_user_email_lower}
 * to satisfy the query. Never query by raw {@code email}: two rows can
 * differ only in casing during the window between the pre-write probe
 * and the {@code @PrePersist} sync, and a raw-email match would miss
 * either of them.
 *
 * <p>Stage 2 (registration) will add {@link #existsByEmailLower(String)}
 * as the service-layer probe before insert.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailLower(String emailLower);

    boolean existsByEmailLower(String emailLower);
}
