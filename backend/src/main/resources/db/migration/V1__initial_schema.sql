-- ============================================================================
--  V1 · Initial schema — Categories, Collections, Products, ProductImages
-- ============================================================================
--  Baseline for Ticket B-INFRA-001. Every table, column, constraint, foreign
--  key and index below corresponds 1:1 to the JPA entity model as of Ticket
--  B002 (Product Module).
--
--  Entities represented:
--    com.eazicut.api.categories.entity.Category
--    com.eazicut.api.collections.entity.Collection
--    com.eazicut.api.products.entity.Product
--    com.eazicut.api.products.entity.ProductImage
--    com.eazicut.api.common.entity.AbstractAuditableEntity
--
--  Portability
--  -----------
--  SQL kept to the intersection of PostgreSQL and H2 (MODE=PostgreSQL) so the
--  same file runs in dev + test (H2 in-memory) and prod (PostgreSQL).
--
--  UUIDs — application-generated (Hibernate `GenerationType.UUID` runs
--  client-side); no `gen_random_uuid()` default. Columns are typed UUID.
--
--  Timestamps — application-set by Spring Data JPA auditing
--  (`@CreatedDate` / `@LastModifiedDate`); no `DEFAULT CURRENT_TIMESTAMP`.
--  Type is `TIMESTAMP(6) WITH TIME ZONE` (matches Hibernate 6 defaults for
--  `Instant`).
--
--  Enums — stored as `VARCHAR(20)` (see `hibernate.type.preferred_enum_jdbc_type`
--  in application.yml).
--
--  Booleans — Java primitive; migration declares NOT NULL without a default
--  because Hibernate always emits the value in every INSERT.
-- ============================================================================


-- ---------------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id          UUID                          NOT NULL PRIMARY KEY,
    name        VARCHAR(120)                  NOT NULL,
    slug        VARCHAR(140)                  NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL
);

CREATE INDEX idx_category_name ON categories (name);


-- ---------------------------------------------------------------------------
-- collections
-- ---------------------------------------------------------------------------
CREATE TABLE collections (
    id          UUID                          NOT NULL PRIMARY KEY,
    name        VARCHAR(120)                  NOT NULL,
    slug        VARCHAR(140)                  NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL
);

CREATE INDEX idx_collection_name ON collections (name);


-- ---------------------------------------------------------------------------
-- products (aggregate root)
--
-- Soft-deleted via `@SQLDelete` + `@SQLRestriction` on the entity: DELETEs
-- against Product are rewritten by Hibernate to
--   UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?
-- and every SELECT is filtered `WHERE deleted_at IS NULL`. The column is
-- indexed for that predicate.
-- ---------------------------------------------------------------------------
CREATE TABLE products (
    id                 UUID                          NOT NULL PRIMARY KEY,
    name               VARCHAR(200)                  NOT NULL,
    slug               VARCHAR(220)                  NOT NULL,
    short_description  VARCHAR(500)                  NOT NULL,
    full_description   TEXT                          NOT NULL,
    sku                VARCHAR(64)                   NOT NULL,
    brand              VARCHAR(100)                  NOT NULL,
    price              NUMERIC(19, 4)                NOT NULL,
    discount_price     NUMERIC(19, 4),
    currency           VARCHAR(3)                    NOT NULL,
    status             VARCHAR(20)                   NOT NULL,
    stock_quantity     INTEGER                       NOT NULL,
    featured           BOOLEAN                       NOT NULL,
    new_arrival        BOOLEAN                       NOT NULL,
    bestseller         BOOLEAN                       NOT NULL,
    fabric_type        VARCHAR(100),
    color              VARCHAR(60),
    category_id        UUID,
    collection_id      UUID,
    deleted_at         TIMESTAMP(6) WITH TIME ZONE,
    created_at         TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    CONSTRAINT uk_product_slug UNIQUE (slug),
    CONSTRAINT uk_product_sku  UNIQUE (sku),
    CONSTRAINT ck_product_status
        CHECK (status IN ('DRAFT','ACTIVE','INACTIVE','OUT_OF_STOCK','ARCHIVED')),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)   REFERENCES categories (id),
    CONSTRAINT fk_product_collection
        FOREIGN KEY (collection_id) REFERENCES collections (id)
);

CREATE INDEX idx_product_name          ON products (name);
CREATE INDEX idx_product_category_id   ON products (category_id);
CREATE INDEX idx_product_collection_id ON products (collection_id);
CREATE INDEX idx_product_status        ON products (status);
CREATE INDEX idx_product_deleted_at    ON products (deleted_at);


-- ---------------------------------------------------------------------------
-- product_images (child of products, one-to-many with orphan removal)
-- ---------------------------------------------------------------------------
CREATE TABLE product_images (
    id          UUID                          NOT NULL PRIMARY KEY,
    product_id  UUID                          NOT NULL,
    url         VARCHAR(2048)                 NOT NULL,
    alt         VARCHAR(500),
    sort_order  INTEGER                       NOT NULL,
    is_primary  BOOLEAN                       NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_image_product_id ON product_images (product_id);


-- ---------------------------------------------------------------------------
-- product_sizes  (@ElementCollection Set<String> — join table)
-- Composite PK (product_id, size) matches Hibernate's default for a Set of
-- scalars, which enforces per-product uniqueness at the DB level.
-- ---------------------------------------------------------------------------
CREATE TABLE product_sizes (
    product_id  UUID          NOT NULL,
    size        VARCHAR(32)   NOT NULL,
    PRIMARY KEY (product_id, size),
    CONSTRAINT fk_product_sizes_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);


-- ---------------------------------------------------------------------------
-- product_tags  (@ElementCollection Set<String> — join table)
-- ---------------------------------------------------------------------------
CREATE TABLE product_tags (
    product_id  UUID          NOT NULL,
    tag         VARCHAR(64)   NOT NULL,
    PRIMARY KEY (product_id, tag),
    CONSTRAINT fk_product_tags_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);
