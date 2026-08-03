"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useAuth } from "@/features/auth/AuthContext";
import { listOrders } from "@/lib/api/orders";
import { ApiAuthError } from "@/lib/api/errors";
import type { ApiOrder } from "@/types/api/orders";

/**
 * Account order history — customer-facing paged list backed by
 * {@code GET /orders}. Middleware gates the whole {@code /account}
 * subtree; the client guard here handles mid-load auth expiry.
 */
export default function AccountOrdersPage() {
  const router = useRouter();
  const { status, getAccessToken } = useAuth();

  const [orders, setOrders] = useState<ApiOrder[] | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [loadState, setLoadState] =
    useState<"loading" | "ready" | "error">("loading");

  useEffect(() => {
    if (status !== "authenticated") return;
    let cancelled = false;
    (async () => {
      setLoadState("loading");
      try {
        const page = await listOrders(getAccessToken(), 0, 20);
        if (!cancelled) {
          setOrders(page.content);
          setTotalElements(page.totalElements);
          setLoadState("ready");
        }
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiAuthError) {
          router.replace(
            `/login?next=${encodeURIComponent("/account/orders")}`
          );
          return;
        }
        setLoadState("error");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [status, getAccessToken, router]);

  if (status !== "authenticated" || loadState === "loading") {
    return (
      <PageShell>
        <p className="text-ink/70">Loading orders…</p>
      </PageShell>
    );
  }
  if (loadState === "error" || !orders) {
    return (
      <PageShell>
        <p className="text-red-700">
          Something went wrong loading your orders. Please try again.
        </p>
      </PageShell>
    );
  }
  if (orders.length === 0) {
    return (
      <PageShell>
        <p className="text-xs uppercase tracking-widest text-gold">
          Your orders
        </p>
        <h1 className="mt-2 font-display text-4xl text-ink">
          You haven&apos;t placed an order yet.
        </h1>
        <p className="mt-4 text-ink/70">
          When you commission a piece, its full history will live here.
        </p>
        <Link
          href="/shop"
          className="mt-8 inline-block border border-ink px-6 py-3 text-sm uppercase tracking-widest text-ink transition hover:bg-ink hover:text-ivory"
        >
          Explore the shop
        </Link>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <p className="text-xs uppercase tracking-widest text-gold">
        Your orders
      </p>
      <h1 className="mt-2 font-display text-4xl text-ink">
        {totalElements === 1 ? "1 order" : `${totalElements} orders`}
      </h1>

      <ul className="mt-12 border-y border-ink/10 divide-y divide-ink/10">
        {orders.map((order) => (
          <li key={order.id}>
            <Link
              href={`/orders/${encodeURIComponent(order.reference)}`}
              className="grid grid-cols-1 sm:grid-cols-[1fr_auto] gap-2 sm:gap-6 py-6 hover:opacity-80 transition"
            >
              <div>
                <p className="font-mono text-sm text-ink">{order.reference}</p>
                <p className="mt-1 text-xs uppercase tracking-widest text-ink/60">
                  {humanStatus(order.status)} ·{" "}
                  {new Date(order.placedAt).toLocaleDateString()}
                </p>
                <p className="mt-2 text-sm text-ink/70">
                  {itemSummary(order.items.length, totalQty(order))}
                </p>
              </div>
              <div className="sm:text-right">
                <p className="text-ink">
                  {formatMoney(order.total, order.currency)}
                </p>
                <p className="mt-1 text-xs uppercase tracking-widest text-ink/60">
                  {order.deliveryMethodName}
                </p>
              </div>
            </Link>
          </li>
        ))}
      </ul>

      {totalElements > orders.length && (
        <p className="mt-8 text-sm text-ink/60">
          Showing the {orders.length} most recent. Pagination for older
          orders will be added shortly.
        </p>
      )}

      <div className="mt-12">
        <Link
          href="/account"
          className="text-sm uppercase tracking-widest text-ink/70 hover:text-ink"
        >
          ← Back to your account
        </Link>
      </div>
    </PageShell>
  );
}

function PageShell({ children }: { children: React.ReactNode }) {
  return <section className="mx-auto max-w-3xl px-6 py-24">{children}</section>;
}

function totalQty(order: ApiOrder): number {
  return order.items.reduce((sum, i) => sum + i.quantity, 0);
}

function itemSummary(lineCount: number, totalQuantity: number): string {
  const lines = lineCount === 1 ? "1 piece" : `${lineCount} pieces`;
  if (totalQuantity === lineCount) return lines;
  return `${lines} · ${totalQuantity} total`;
}

function humanStatus(status: string): string {
  switch (status) {
    case "PENDING_PAYMENT":
      return "Pending payment";
    case "PAID":
      return "Paid";
    case "FULFILLING":
      return "In the atelier";
    case "SHIPPED":
      return "Shipped";
    case "DELIVERED":
      return "Delivered";
    case "CANCELLED":
      return "Cancelled";
    case "REFUNDED":
      return "Refunded";
    default:
      return status;
  }
}

function formatMoney(amount: string, currency: string): string {
  const n = Number(amount);
  if (!Number.isFinite(n)) return `${amount} ${currency}`;
  const symbol = currency === "NGN" ? "₦" : `${currency} `;
  return `${symbol}${n.toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`;
}
