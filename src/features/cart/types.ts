import type { Product } from "@/types/product";

/**
 * A single line in the customer's cart.
 *
 * `snapshot` captures a point-in-time copy of the product (name, price, image,
 * slug) so the cart survives a catalogue rename or a fabric swap between the
 * time of adding and the time of checkout. The full Product record is not
 * stored — only the fields the cart UI needs.
 */
export type CartSnapshot = Pick<
  Product,
  "slug" | "name" | "price" | "image" | "collection" | "category"
>;

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
 * Navbar badge, checkout) will bind to. Ticket 002.4 will lean on the
 * additional operations reserved below.
 */
export type CartApi = {
  lines: CartLine[];
  count: number;
  subtotal: number;
  add: (product: CartSnapshot, size: string, quantity?: number) => void;
  remove: (id: string) => void;
  setQuantity: (id: string, quantity: number) => void;
  clear: () => void;
};
