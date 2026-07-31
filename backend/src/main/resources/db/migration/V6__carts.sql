-- ---------------------------------------------------------------------------
-- V6 — shopping carts (B005 Stage 1)
--
-- Design highlights.
--
-- One active cart per user (ux_cart_user unique index). Multi-cart /
-- saved-for-later is a later feature; for launch every customer has
-- exactly one bag.
--
-- Cart line identity is (cart_id, product_id, size) — same rule the
-- pre-B005 localStorage cart already used. A customer can hold two
-- lines of the same piece in different sizes. Enforced with a UNIQUE
-- index so add() becomes an upsert.
--
-- Snapshot columns capture the product's name, slug, price, currency,
-- and image at the moment of add. The cart page and checkout render
-- from these fields — a mid-cart product rename or reprice never
-- reshapes what the customer sees.
--
-- CRITICAL invariant. The snapshot price is HISTORICAL / DISPLAY only.
-- It is NEVER used as the charged price on an Order. The Order
-- pipeline (later ticket) must:
--   1. Compare snapshot_price with the current products.price.
--   2. If different, force the customer to explicitly confirm.
--   3. Persist the confirmed price into an OrderItem row — that becomes
--      the immutable, chargeable amount.
-- A stale cart snapshot must never silently become the charged price.
--
-- product_id FK is intentionally NOT ON DELETE CASCADE. Products are
-- soft-deleted (@SQLDelete + @SQLRestriction) so the row never
-- physically vanishes; the FK stays satisfied. If a hard delete is
-- ever performed manually, the cart_items row must be resolved
-- (removed or reassigned) rather than silently disappearing — the
-- deletion will fail loudly, which is the safe outcome.
--
-- ON DELETE CASCADE from carts → cart_items keeps the child rows in
-- sync when a cart is wiped, and from users → carts covers the
-- future account-deletion path.
--
-- ck_cart_item_qty caps per-line quantity at 20. Above that is
-- effectively wholesale ordering, out of scope for a bespoke atelier.
--
-- Safety. New tables only; empty on every environment at migration
-- time.
-- ---------------------------------------------------------------------------

CREATE TABLE carts (
    id          UUID                          NOT NULL PRIMARY KEY,
    user_id     UUID                          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency    VARCHAR(3)                    NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL
);

CREATE UNIQUE INDEX ux_cart_user ON carts (user_id);


CREATE TABLE cart_items (
    id                  UUID                          NOT NULL PRIMARY KEY,
    cart_id             UUID                          NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id          UUID                          NOT NULL REFERENCES products(id),
    size                VARCHAR(32)                   NOT NULL,
    quantity            INTEGER                       NOT NULL,

    -- Point-of-add snapshot — historical/display only. See file header
    -- for the "never silently becomes charged price" invariant.
    snapshot_name       VARCHAR(200)                  NOT NULL,
    snapshot_slug       VARCHAR(220)                  NOT NULL,
    snapshot_price      NUMERIC(19, 4)                NOT NULL,
    snapshot_currency   VARCHAR(3)                    NOT NULL,
    snapshot_image_url  VARCHAR(2048),

    added_at            TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    updated_at          TIMESTAMP(6) WITH TIME ZONE   NOT NULL,

    CONSTRAINT ck_cart_item_qty CHECK (quantity > 0 AND quantity <= 20)
);

CREATE UNIQUE INDEX ux_cart_item_line ON cart_items (cart_id, product_id, size);
CREATE INDEX idx_cart_item_product   ON cart_items (product_id);
