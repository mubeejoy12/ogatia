import type { Product } from "@/types/product";
import type { ApiCartIssue } from "@/types/api/cart";

/**
 * A single line in the customer's cart.
 *
 * `snapshot` captures a point-in-time copy of the product (name, price,
 * image, slug, id) so the cart survives a catalogue rename or a fabric
 * swap between the time of adding and the time of checkout. The full
 * Product record is not stored — only the fields the cart UI needs.
 *
 * <p>{@code productId} is optional to keep legacy mock-data compatibility;
 * every API-adapted product carries it, and it's required for server-side
 * cart mutations (B005). Guest-mode carts use the slug for line identity.
 */
export type CartSnapshot = Pick<
  Product,
  "slug" | "name" | "price" | "image" | "collection" | "category"
> & {
  /** Backend product id — required when the cart is server-backed. */
  productId?: string;
};

export type CartLine = {
  /** Composite id — `${slug}::${size}` — because a customer may add the same
   *  piece in two sizes. */
  id: string;
  slug: Product["slug"];
  size: string;
  quantity: number;
  addedAt: string; // ISO
  snapshot: CartSnapshot;
};

export type CartState = {
  lines: CartLine[];
};

/**
 * Public cart API — the shape every consumer (ProductActions, Cart page,
 * Navbar badge, checkout) binds to.
 *
 * <p>B005 preserved the pre-existing surface (add / remove / setQuantity /
 * clear) so PDP + checkout continued to compile without edits. Two
 * additions:
 *
 * <ul>
 *   <li>{@code issues} — server-side warnings (price changed, out of
 *       stock, product removed, quantity capped). Empty in guest mode.</li>
 *   <li>{@code mode} — {@code "guest"} for localStorage-backed carts or
 *       {@code "server"} for authenticated ones. Consumers rarely need
 *       to branch on this; the shared API works for both.</li>
 * </ul>
 *
 * <p>Mutations are fire-and-forget on the server side — errors are
 * logged; state updates when the server responds. Optimistic UI is
 * out of B005 scope.
 */
export type CartMode = "guest" | "server";

export type CartApi = {
  lines: CartLine[];
  count: number;
  subtotal: number;
  mode: CartMode;
  issues: ApiCartIssue[];
  add: (product: CartSnapshot, size: string, quantity?: number) => void;
  remove: (id: string) => void;
  setQuantity: (id: string, quantity: number) => void;
  clear: () => void;
};
