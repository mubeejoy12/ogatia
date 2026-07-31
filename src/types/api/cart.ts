/**
 * Mirror of the backend cart DTOs
 * ({@code com.eazicut.api.cart.dto.*}).
 *
 * <p>Field names match the JSON on the wire verbatim — do not rename
 * without updating the backend at the same time.
 *
 * <p><strong>Snapshot ≠ charged price.</strong> The {@code snapshot.price}
 * captured at add time is HISTORICAL / DISPLAY only. The charged price
 * is decided at Order time; a stale snapshot must never silently become
 * the amount the customer is billed. See CartItem entity Javadoc on the
 * backend.
 */

export type ApiCartItemSnapshot = {
  name: string;
  slug: string;
  price: string;            // BigDecimal serialised as string
  currency: string;
  imageUrl: string | null;
};

/**
 * A cart line's issue code. Kept as a union of strings rather than a
 * TS enum so the backend can add new codes without breaking the client
 * type-check — unknown codes render with the server-provided message.
 */
export type ApiCartIssueCode =
  | "price_changed"
  | "out_of_stock"
  | "product_removed"
  | "size_unavailable"
  | "quantity_capped"
  | "cart_too_large";

export type ApiCartIssue = {
  /** Item this issue refers to. Null for cart-level toasts (merge-time). */
  itemId: string | null;
  code: ApiCartIssueCode | string;
  message: string;
};

export type ApiCartItem = {
  id: string;
  productId: string;
  productSlug: string;
  size: string;
  quantity: number;
  snapshot: ApiCartItemSnapshot;
  currentPrice: string;     // BigDecimal as string
  available: boolean;
  addedAt: string;          // ISO Instant
  updatedAt: string;
};

export type ApiCart = {
  id: string;
  currency: string;
  items: ApiCartItem[];
  subtotal: string;         // BigDecimal as string
  itemCount: number;
  issues: ApiCartIssue[];
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Requests
// ---------------------------------------------------------------------------

export type ApiAddCartItemRequest = {
  productId: string;
  size: string;
  quantity: number;
};

export type ApiUpdateCartItemRequest = {
  quantity: number;
};

export type ApiMergeCartLine = {
  productSlug: string;
  size: string;
  quantity: number;
};

export type ApiMergeCartRequest = {
  lines: ApiMergeCartLine[];
};
