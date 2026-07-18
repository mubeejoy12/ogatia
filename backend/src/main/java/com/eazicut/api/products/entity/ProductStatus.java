package com.eazicut.api.products.entity;

/**
 * Lifecycle status for a product.
 *
 * <p>Persisted as {@code STRING} so the underlying storage remains
 * human-readable and safe to reorder / extend without a data migration.
 *
 * <ul>
 *   <li>{@link #DRAFT} — created but not yet published; hidden from every public read.</li>
 *   <li>{@link #ACTIVE} — published, purchasable when {@code stockQuantity > 0}.</li>
 *   <li>{@link #INACTIVE} — temporarily withdrawn (seasonal, being re-photographed); returns
 *       404 to the public but remains editable by staff.</li>
 *   <li>{@link #OUT_OF_STOCK} — visible but not purchasable; shown as "By enquiry".</li>
 *   <li>{@link #ARCHIVED} — retired; kept for order history and analytics only.</li>
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK,
    ARCHIVED
}
