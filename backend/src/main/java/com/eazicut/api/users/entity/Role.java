package com.eazicut.api.users.entity;

/**
 * The application roles recognised at launch.
 *
 * <p>Persisted as VARCHAR via {@code @Enumerated(EnumType.STRING)} +
 * {@code @JdbcTypeCode(SqlTypes.VARCHAR)} on {@link User#role} — matches
 * the {@code ProductStatus} pattern so H2 and PostgreSQL agree on the
 * storage shape.
 *
 * <p>Mapping to Spring Security authorities:
 *
 * <ul>
 *   <li>{@link #CUSTOMER} → {@code ROLE_CUSTOMER}</li>
 *   <li>{@link #ADMIN}    → {@code ROLE_ADMIN}</li>
 * </ul>
 *
 * Spring's {@code hasRole('ADMIN')} implicitly looks for
 * {@code ROLE_ADMIN}, so the {@link #authority()} helper prefixes
 * once, at the authority-adaptation boundary in
 * {@code UserDetailsAdapter}.
 *
 * <p>Adding a role is a Java-only change; the {@code users.role} column
 * is a plain VARCHAR(32) so no migration is needed to introduce e.g.
 * {@code STAFF} or {@code SUPER_ADMIN}.
 */
public enum Role {
    CUSTOMER,
    ADMIN;

    /** Spring Security authority string, always prefixed {@code ROLE_}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
