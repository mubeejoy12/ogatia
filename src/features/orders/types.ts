import type { CartLine } from "@/features/cart/types";
import type {
  DeliveryMethodId,
} from "@/features/checkout/deliveryMethods";
import type { ShippingAddress } from "@/features/checkout/types";

/**
 * A completed order.
 *
 * Persisted to localStorage keyed by `reference` — atelier team looks up the
 * same reference on the Paystack dashboard to verify payment. When a
 * backend + database are introduced this shape becomes what the API
 * returns, so consumers won't need to change.
 */
export type Order = {
  /** Paystack reference, e.g. "EAZI-1720617483471-A9F3C1". */
  reference: string;
  /** ISO 8601 timestamp of order completion (client clock). */
  placedAt: string;
  /** Snapshot of the bag contents at the moment of payment. */
  lines: CartLine[];
  /** Amount fields — captured so refunds & receipts don't have to recompute. */
  subtotal: number;
  shipping: number;
  total: number;
  /** Chosen shipping method id + name so a rename later doesn't lose history. */
  deliveryMethodId: DeliveryMethodId;
  deliveryMethodName: string;
  /** Snapshot of the shipping address. */
  address: ShippingAddress;
  /** Payment status. `pending-verification` is the launch reality until the
   *  atelier or a webhook confirms the transaction on the Paystack side. */
  status: "pending-verification" | "confirmed" | "refunded" | "failed";
};
