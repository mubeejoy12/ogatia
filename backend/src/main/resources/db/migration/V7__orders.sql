-- ---------------------------------------------------------------------------
-- V7 — orders (B006 Stage 1)
--
-- Design highlights.
--
-- Reference. Human-facing identifier `EAZI-<epoch>-<hex>` — matches the
-- existing localStorage prototype so support staff don't have to retrain.
-- Enforced UNIQUE at the DB level; the service regenerates on collision
-- with a bounded retry.
--
-- Status. Full lifecycle enum encoded as VARCHAR + CHECK constraint —
-- same portable pattern V1 uses for ProductStatus. B006 only enforces
-- PENDING_PAYMENT and CANCELLED transitions; PAID / FULFILLING /
-- SHIPPED / DELIVERED / REFUNDED are reserved so B007 (payment) and
-- future admin fulfilment can move through them without a schema
-- migration.
--
-- Money. NUMERIC(19,4) throughout — same convention as products.price.
-- Never widen to floating-point (audit trail on ₦850,000 tuxedos).
--
-- Idempotency. `idempotency_key` is REQUIRED (per D4). Uniqueness is
-- scoped by (user_id, idempotency_key) so keys can't cross accounts.
-- A plain composite UNIQUE index works portably on H2 v2 and
-- PostgreSQL — no partial-index syntax needed because the column is
-- NOT NULL by contract. The service layer probes before insert; the
-- DB constraint is the concurrent-write backstop.
--
-- Shipping address. Embedded as columns on `orders` rather than a
-- separate table — no address book at launch, and orders capture
-- shipping details at a point in time (a later address-book ticket
-- would seed itself FROM these order rows, not the other way round).
--
-- Delivery method. Denormalised — captures id + name at order time so
-- a later rename or price change in `deliveryMethods.ts` doesn't
-- rewrite order history.
--
-- FK. `order_items.product_id → products.id` WITHOUT cascade delete.
-- Products are soft-deleted (@SQLDelete + @SQLRestriction) so the row
-- never physically vanishes; the FK stays satisfied. `orders.user_id →
-- users.id` is also non-cascading; a hard user delete would fail
-- loudly, which is the safe outcome for compliance / audit.
--
-- `order_items` FK to `orders` DOES cascade — an order is a natural
-- aggregate root; deleting an order (rare, admin-only path) removes
-- its lines.
--
-- Safety. New tables only; empty on every environment at migration
-- time.
-- ---------------------------------------------------------------------------

CREATE TABLE orders (
    id                       UUID                          NOT NULL PRIMARY KEY,
    reference                VARCHAR(40)                   NOT NULL,
    user_id                  UUID                          NOT NULL REFERENCES users(id),
    status                   VARCHAR(32)                   NOT NULL,
    currency                 VARCHAR(3)                    NOT NULL,
    subtotal                 NUMERIC(19, 4)                NOT NULL,
    shipping_cost            NUMERIC(19, 4)                NOT NULL,
    total                    NUMERIC(19, 4)                NOT NULL,

    -- Delivery method (denormalised snapshot)
    delivery_method_id       VARCHAR(64)                   NOT NULL,
    delivery_method_name     VARCHAR(200)                  NOT NULL,

    -- Shipping address (embedded — see file header)
    shipping_full_name       VARCHAR(200)                  NOT NULL,
    shipping_email           VARCHAR(255)                  NOT NULL,
    shipping_phone           VARCHAR(50)                   NOT NULL,
    shipping_address_line1   VARCHAR(300)                  NOT NULL,
    shipping_address_line2   VARCHAR(300),
    shipping_city            VARCHAR(100)                  NOT NULL,
    shipping_region          VARCHAR(100),
    shipping_postal_code     VARCHAR(30),
    shipping_country         VARCHAR(100)                  NOT NULL,

    -- Idempotency — required by contract (see D4). Composite unique
    -- with user_id below.
    idempotency_key          VARCHAR(128)                  NOT NULL,

    placed_at                TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    created_at               TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at               TIMESTAMP(6) WITH TIME ZONE   NOT NULL,

    CONSTRAINT ck_order_status CHECK (status IN (
        'PENDING_PAYMENT','PAID','FULFILLING','SHIPPED','DELIVERED','CANCELLED','REFUNDED'
    ))
);

CREATE UNIQUE INDEX ux_order_reference        ON orders (reference);
CREATE UNIQUE INDEX ux_order_idempotency_user ON orders (user_id, idempotency_key);
CREATE INDEX        idx_order_user            ON orders (user_id);
CREATE INDEX        idx_order_status          ON orders (status);


CREATE TABLE order_items (
    id                UUID                          NOT NULL PRIMARY KEY,
    order_id          UUID                          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id        UUID                          NOT NULL REFERENCES products(id),

    -- Snapshot of what was actually charged. Immutable per business
    -- rule: once an order line is written, product identity + price
    -- are frozen for legal / accounting / refund purposes.
    product_slug      VARCHAR(220)                  NOT NULL,
    product_name      VARCHAR(200)                  NOT NULL,
    product_image_url VARCHAR(2048),

    size              VARCHAR(32)                   NOT NULL,
    quantity          INTEGER                       NOT NULL,
    unit_price        NUMERIC(19, 4)                NOT NULL,
    currency          VARCHAR(3)                    NOT NULL,
    line_total        NUMERIC(19, 4)                NOT NULL,

    created_at        TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE   NOT NULL,

    CONSTRAINT ck_order_item_qty CHECK (quantity > 0)
);

CREATE INDEX idx_order_item_order   ON order_items (order_id);
CREATE INDEX idx_order_item_product ON order_items (product_id);
