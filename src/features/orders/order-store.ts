import type { Order } from "./types";

const STORAGE_KEY = "eazicut:orders:v1";

type OrderMap = Record<string, Order>;

/**
 * A thin, SSR-safe accessor over localStorage-persisted orders.
 *
 * Keyed by reference so the confirmation page can idempotently render on
 * refresh without duplicating the order or re-clearing the cart.
 *
 * When the backend takes over these functions become fetch calls to
 * /api/orders — the signatures stay identical, so ConfirmationView (and
 * later an /account/orders page) will not change.
 */

function readAll(): OrderMap {
  if (typeof window === "undefined") return {};
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as OrderMap;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function writeAll(orders: OrderMap): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(orders));
  } catch {
    // quota / private mode — ignore
  }
}

export function getOrder(reference: string): Order | null {
  return readAll()[reference] ?? null;
}

export function saveOrder(order: Order): void {
  const orders = readAll();
  orders[order.reference] = order;
  writeAll(orders);
}
