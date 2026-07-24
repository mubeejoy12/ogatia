-- ---------------------------------------------------------------------------
-- V2 — case-insensitive uniqueness on category name
--
-- Rationale. The categories table already carries UNIQUE(slug); B003 also
-- requires the human-facing name to be unique regardless of case ("Suits"
-- and "suits" are the same category). Enforced in two layers:
--
--   1. Service layer — CategoryService probes existsByNameIgnoreCase before
--      every save so most collisions become a clean 409 with a helpful
--      message before any INSERT is attempted.
--
--   2. Database layer (this migration) — the backstop for concurrent writes
--      that slip past the probe. Under a race the constraint fires and
--      GlobalExceptionHandler maps the resulting DataIntegrityViolationException
--      to a 409 with the same shape.
--
-- Portability. We can't use a functional unique index (H2 v2 rejects function
-- calls inside CREATE INDEX) or a native GENERATED column (H2 v2 supports
-- only VIRTUAL, PostgreSQL only STORED). Instead we add a plain
-- `name_lower` column that the Category entity populates from `name` via
-- @PrePersist / @PreUpdate; the DB indexes that column uniquely. This is
-- 100% standard SQL and works identically on H2 and PostgreSQL.
--
-- Safety. The categories table has been admin-write-only since B002 and no
-- seed data has been shipped yet, so at migration time it is empty on
-- every environment. The UPDATE below is a defensive no-op that also keeps
-- the migration correct if this ever runs against a non-empty table.
-- ---------------------------------------------------------------------------

ALTER TABLE categories ADD COLUMN name_lower VARCHAR(120);

UPDATE categories SET name_lower = LOWER(name) WHERE name_lower IS NULL;

ALTER TABLE categories ALTER COLUMN name_lower SET NOT NULL;

CREATE UNIQUE INDEX ux_category_name_lower ON categories (name_lower);
