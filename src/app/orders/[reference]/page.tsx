"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useAuth } from "@/features/auth/AuthContext";
import { getOrderByReference } from "@/lib/api/orders";
import { ApiAuthError, ApiClientError } from "@/lib/api/errors";
import type { ApiOrder } from "@/types/api/orders";

/**
 * Canonical order detail page — works both as the immediate
 * post-checkout confirmation and as an "open my order" view from
 * an email link. Reference is the URL segment; the API resolves
 * scoped to the caller.
 *
 * <p>Anonymous visitors are bounced to
 * {@code /login?next=/orders/[ref]} by the middleware; the client
 * guard here covers the edge where a valid Bearer expires
 * mid-load.
 */
export default function OrderDetailPage() {
  const params = useParams<{ reference: string }>();
  const reference = decodeURIComponent(params.reference);
  const router = useRouter();
  const { status, getAccessToken } = useAuth();

  const [order, setOrder] = useState<ApiOrder | null>(null);
  const [loadState, setLoadState] =
    useState<"loading" | "ready" | "not-found" | "error">("loading");

  useEffect(() => {
    if (status !== "authenticated") return;
    let cancelled = false;
    (async () => {
      setLoadState("loading");
      try {
        const fetched = await getOrderByReference(getAccessToken(), reference);
        if (!cancelled) {
          setOrder(fetched);
          setLoadState("ready");
        }
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiAuthError) {
          router.replace(
            `/login?next=${encodeURIComponent(`/orders/${reference}`)}`
          );
          return;
        }
        if (err instanceof ApiClientError && err.status === 404) {
          setLoadState("not-found");
          return;
        }
        setLoadState("error");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [status, reference, getAccessToken, router]);

  if (status === "anonymous") {
    // Middleware normally redirects before this renders; belt-and-braces.
    router.replace(`/login?next=${encodeURIComponent(`/orders/${reference}`)}`);
    return <PageShell><p className="text-ink/70">Redirecting…</p></PageShell>;
  }
  if (status === "loading" || loadState === "loading") {
    return <PageShell><p className="text-ink/70">Loading order…</p></PageShell>;
  }
  if (loadState === "not-found") {
    return (
      <PageShell>
        <h1 className="font-display text-3xl text-ink">Order not found</h1>
        <p className="mt-4 text-ink/70">
          We couldn&apos;t find an order with reference{" "}
          <code className="font-mono">{reference}</code>. If you were expecting
          one, please check your email or contact the atelier.
        </p>
        <Link
          href="/account/orders"
          className="mt-8 inline-block border border-ink px-6 py-3 text-sm uppercase tracking-widest text-ink transition hover:bg-ink hover:text-ivory"
        >
          Your orders
        </Link>
      </PageShell>
    );
  }
  if (loadState === "error" || !order) {
    return (
      <PageShell>
        <h1 className="font-display text-3xl text-ink">Something went wrong</h1>
        <p className="mt-4 text-ink/70">
          Please try again in a moment or contact the atelier.
        </p>
      </PageShell>
    );
  }

  const isConfirmation = order.status === "PENDING_PAYMENT";

  return (
    <PageShell>
      <p className="text-xs uppercase tracking-widest text-gold">
        {isConfirmation ? "Order placed" : "Order"}
      </p>
      <h1 className="mt-2 font-display text-4xl text-ink">
        {isConfirmation
          ? "Thank you — your order is with the atelier."
          : `Order ${order.reference}`}
      </h1>
      {isConfirmation && (
        <p className="mt-3 text-ink/70">
          Reference <span className="font-mono">{order.reference}</span> — a
          member of the atelier team will confirm the details and payment
          next.
        </p>
      )}

      <div className="mt-10 grid grid-cols-1 sm:grid-cols-3 gap-6 border-t border-ink/10 pt-8">
        <Fact label="Status" value={humanStatus(order.status)} />
        <Fact label="Placed" value={new Date(order.placedAt).toLocaleString()} />
        <Fact
          label="Total"
          value={formatMoney(order.total, order.currency)}
        />
      </div>

      <section className="mt-14">
        <h2 className="font-display text-2xl text-ink">Pieces</h2>
        <ul className="mt-6 border-y border-ink/10 divide-y divide-ink/10">
          {order.items.map((item) => (
            <li
              key={item.id}
              className="flex items-start justify-between gap-6 py-6"
            >
              <div>
                <p className="font-medium text-ink">{item.productName}</p>
                <p className="mt-1 text-xs uppercase tracking-widest text-ink/60">
                  Size {item.size} · Qty {item.quantity}
                </p>
              </div>
              <p className="text-ink whitespace-nowrap">
                {formatMoney(item.lineTotal, item.currency)}
              </p>
            </li>
          ))}
        </ul>
        <dl className="mt-8 space-y-2 text-sm">
          <div className="flex justify-between text-ink/70">
            <dt>Subtotal</dt>
            <dd>{formatMoney(order.subtotal, order.currency)}</dd>
          </div>
          <div className="flex justify-between text-ink/70">
            <dt>{order.deliveryMethodName}</dt>
            <dd>{formatMoney(order.shippingCost, order.currency)}</dd>
          </div>
          <div className="flex justify-between text-ink font-medium pt-2 border-t border-ink/10">
            <dt>Total</dt>
            <dd>{formatMoney(order.total, order.currency)}</dd>
          </div>
        </dl>
      </section>

      <section className="mt-14">
        <h2 className="font-display text-2xl text-ink">Shipping to</h2>
        <address className="mt-4 not-italic text-ink/80 leading-relaxed">
          {order.shippingAddress.fullName}
          <br />
          {order.shippingAddress.addressLine1}
          {order.shippingAddress.addressLine2 && (
            <>
              <br />
              {order.shippingAddress.addressLine2}
            </>
          )}
          <br />
          {[
            order.shippingAddress.city,
            order.shippingAddress.region,
            order.shippingAddress.postalCode,
          ]
            .filter(Boolean)
            .join(" · ")}
          <br />
          {order.shippingAddress.country}
        </address>
        <p className="mt-4 text-sm text-ink/60">
          {order.shippingAddress.email} · {order.shippingAddress.phone}
        </p>
      </section>

      <div className="mt-12 flex flex-wrap gap-3">
        <Link
          href="/account/orders"
          className="border border-ink px-6 py-3 text-sm uppercase tracking-widest text-ink transition hover:bg-ink hover:text-ivory"
        >
          Your orders
        </Link>
        <Link
          href="/shop"
          className="border border-ink/20 px-6 py-3 text-sm uppercase tracking-widest text-ink/80 transition hover:border-ink"
        >
          Continue shopping
        </Link>
      </div>
    </PageShell>
  );
}

function PageShell({ children }: { children: React.ReactNode }) {
  return <section className="mx-auto max-w-3xl px-6 py-24">{children}</section>;
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-widest text-ink/60">{label}</dt>
      <dd className="mt-1 text-ink">{value}</dd>
    </div>
  );
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
