-- ---------------------------------------------------------------------------
-- V3 — case-insensitive uniqueness on collection name
--
-- Direct parallel of V2 (categories). See V2 for the full design rationale:
-- two-layer enforcement (service probe + DB backstop), and the reason we
-- use a plain `name_lower` column populated by the entity's @PrePersist /
-- @PreUpdate rather than a functional index or a DB-generated column
-- (H2 v2 and PostgreSQL disagree on the syntax for both alternatives).
--
-- Safety. The collections table has been admin-write-only since B002 and
-- no seed data has been shipped yet, so at migration time it is empty on
-- every environment. The UPDATE below is a defensive no-op that also
-- keeps the migration correct if this ever runs against a non-empty table.
-- ---------------------------------------------------------------------------

ALTER TABLE collections ADD COLUMN name_lower VARCHAR(120);

UPDATE collections SET name_lower = LOWER(name) WHERE name_lower IS NULL;

ALTER TABLE collections ALTER COLUMN name_lower SET NOT NULL;

CREATE UNIQUE INDEX ux_collection_name_lower ON collections (name_lower);
