import type { Product } from "@/types/product";

/**
 * Human-readable delivery timeline for a product.
 *
 * The backend does not yet carry a per-product delivery-time field; the
 * heuristic below derives it from the merchandising signals already on
 * the product (badge + availability). When B-PRODUCT-DELIVERY (or similar)
 * ships and Product gains a real field, the caller signature stays the same.
 *
 * <p><strong>Note on the "Bespoke" branch:</strong> the current API
 * adapter maps only {@code newArrival → "New"}. Until the backend
 * exposes a badge concept richer than three booleans, the Bespoke path
 * is unreachable from live data; it stays here so the function keeps
 * a full switch for any local / mock consumers.
 */
export function getDeliveryEstimate(product: Product): string {
  if (product.badge === "Bespoke")
    return "4 – 6 weeks (bespoke, cut to measure)";
  if (!product.available) return "By enquiry — write to the atelier";
  return "3 – 5 business days (ready-to-wear, ships from Lagos)";
}
