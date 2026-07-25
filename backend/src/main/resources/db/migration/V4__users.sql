-- ---------------------------------------------------------------------------
-- V4 — users table (B004 Stage 1)
--
-- Rationale. The Spring Boot in-memory user store (spring.security.user.*)
-- is retired in this ticket. Real authentication requires a persistent
-- User record with a hashed password and a role.
--
-- Uniqueness. Email uniqueness is case-insensitive ("Admin@Eazicut.com"
-- and "admin@eazicut.com" are the same account) and enforced in two
-- layers — service probe + DB backstop — using the same name_lower
-- pattern V2/V3 established for categories/collections. That pattern
-- is portable across H2 v2 and PostgreSQL, which functional indexes
-- and GENERATED columns are not.
--
-- Password storage. `password_hash` is sized 72 chars — the exact output
-- width of BCrypt when serialised in Modular Crypt Format. Argon2 or a
-- future upgrade would need a widening migration.
--
-- Role. Kept as VARCHAR + application-side enum (@Enumerated STRING,
-- @JdbcTypeCode VARCHAR — same shape as ProductStatus). Two values for
-- launch: CUSTOMER (default on registration) and ADMIN (seeded, or
-- promoted manually until the admin UI ships). Extending the set means
-- adding a value to the Java enum; no schema change required.
--
-- Enabled flag. Reserved for the account-disable path (compliance,
-- moderation, self-delete). No UI wired to it in B004 — Stage 5 uses
-- it when refresh-token revocation cascades on disable.
--
-- Safety. New table only; no data to migrate; empty on every environment
-- at migration time.
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id             UUID                          NOT NULL PRIMARY KEY,
    email          VARCHAR(255)                  NOT NULL,
    email_lower    VARCHAR(255)                  NOT NULL,
    password_hash  VARCHAR(72)                   NOT NULL,
    display_name   VARCHAR(120),
    role           VARCHAR(32)                   NOT NULL,
    enabled        BOOLEAN                       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE   NOT NULL
);

CREATE UNIQUE INDEX ux_user_email_lower ON users (email_lower);
CREATE INDEX idx_user_role ON users (role);
