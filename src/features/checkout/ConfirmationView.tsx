"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useCart } from "@/features/cart/CartContext";
import { formatNaira } from "@/lib/format";
import { getOrder, saveOrder } from "@/features/orders/order-store";
import type { Order } from "@/features/orders/types";
import {
  deliveryMethods,
  getDeliveryMethod,
  type DeliveryMethodId,
} from "./deliveryMethods";
import { emptyAddress, type ShippingAddress } from "./types";

const ADDRESS_KEY = "eazicut:checkout:address:v1";
const DELIVERY_KEY = "eazicut:checkout:delivery:v1";

/**
 * The order confirmation surface.
 *
 * Behaviour:
 *  1. Read `ref` from the URL.
 *  2. If an order with that ref already exists in localStorage → render it
 *     (idempotent refresh; cart is already clear).
 *  3. Otherwise: materialise the order from `useCart` + persisted address
 *     + persisted delivery method, write it to the order store, then
 *     clear the cart.
 *  4. If no cart lines exist AND no persisted order for the ref exists
 *     (visitor pasted a URL, or hit refresh after storage was wiped) →
 *     render a graceful "no order found" state.
 */
export function ConfirmationView() {
  const cart = useCart();
  const searchParams = useSearchParams();
  const reference = searchParams.get("ref") ?? "";

  const [order, setOrder] = useState<Order | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;

    if (!reference) {
      setNotFound(true);
      return;
    }

    // 1. Existing order — idempotent refresh path.
    const existing = getOrder(reference);
    if (existing) {
      setOrder(existing);
      return;
    }

    // 2. New order — construct from current cart + persisted checkout state.
    if (cart.lines.length === 0) {
      setNotFound(true);
      return;
    }

    let address: ShippingAddress = emptyAddress;
    let deliveryId: DeliveryMethodId | null = null;
    try {
      const rawAddr = window.localStorage.getItem(ADDRESS_KEY);
      if (rawAddr) address = { ...emptyAddress, ...JSON.parse(rawAddr) };
      const rawDelivery = window.localStorage.getItem(DELIVERY_KEY);
      if (rawDelivery) deliveryId = rawDelivery as DeliveryMethodId;
    } catch {
      /* ignore */
    }

    const method = getDeliveryMethod(deliveryId) ?? deliveryMethods[0];
    const shipping = method.price;
    const total = cart.subtotal + shipping;

    const newOrder: Order = {
      reference,
      placedAt: new Date().toISOString(),
      lines: cart.lines,
      subtotal: cart.subtotal,
      shipping,
      total,
      deliveryMethodId: method.id,
      deliveryMethodName: method.name,
      address,
      status: "pending-verification",
    };

    saveOrder(newOrder);
    setOrder(newOrder);
    // Clear the bag last, once the order is safely persisted.
    cart.clear();
  }, [hydrated, reference, cart]);

  const formattedDate = useMemo(() => {
    if (!order) return "";
    try {
      return new Intl.DateTimeFormat("en-NG", {
        weekday: "long",
        year: "numeric",
        month: "long",
        day: "numeric",
      }).format(new Date(order.placedAt));
    } catch {
      return order.placedAt;
    }
  }, [order]);

  if (!hydrated) return <div aria-hidden className="min-h-[50vh]" />;

  if (notFound) return <ConfirmationNotFound reference={reference} />;

  if (!order) return <div aria-hidden className="min-h-[50vh]" />;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-start">
      {/* Order detail */}
      <div className="lg:col-span-8">
        <p className="eyebrow text-stone-500">
          <span className="rule inline-block align-middle mr-3" />
          Order received · {formattedDate}
        </p>

        <h2 className="mt-5 font-display text-3xl md:text-4xl tracking-tightest">
          Thank you for your commission.
        </h2>
        <p className="mt-6 text-stone-700 leading-relaxed max-w-xl">
          A senior member of the atelier will confirm your order within one
          business day. Your reference is below — please keep it for any
          correspondence with the atelier.
        </p>

        <div className="mt-8 inline-block border border-ink/20 px-6 py-4 bg-stone-50">
          <p className="eyebrow text-stone-500">Reference</p>
          <p className="mt-1 font-display text-2xl tracking-tight text-ink select-all">
            {order.reference}
          </p>
        </div>

        {/* Line items */}
        <section aria-labelledby="items-heading" className="mt-14">
          <h3
            id="items-heading"
            className="eyebrow text-stone-500"
          >
            <span className="rule inline-block align-middle mr-3" />
            Your pieces
          </h3>
          <ul className="mt-6 divide-y divide-ink/10 border-t border-ink/10">
            {order.lines.map((line) => (
              <li key={line.id} className="flex gap-4 py-5">
                <div className="relative aspect-[4/5] w-16 shrink-0 overflow-hidden bg-stone-100">
                  <Image
                    src={line.snapshot.image.src}
                    alt={line.snapshot.image.alt}
                    fill
                    sizes="64px"
                    className="object-cover"
                  />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-display text-base tracking-tightest">
                    {line.snapshot.name}
                  </p>
                  <p className="mt-1 text-xs uppercase tracking-widest text-stone-500">
                    Size {line.size} · Qty {line.quantity}
                  </p>
                </div>
                <p className="font-display text-sm tracking-tight text-ink whitespace-nowrap">
                  {formatNaira(line.snapshot.price * line.quantity)}
                </p>
              </li>
            ))}
          </ul>
        </section>

        {/* Shipping + delivery */}
        <section
          aria-labelledby="ship-heading"
          className="mt-14 grid grid-cols-1 md:grid-cols-2 gap-10"
        >
          <div>
            <h3 id="ship-heading" className="eyebrow text-stone-500">
              <span className="rule inline-block align-middle mr-3" />
              Shipping to
            </h3>
            <address className="mt-4 not-italic text-ink leading-relaxed">
              <p className="font-medium">{order.address.fullName}</p>
              <p>{order.address.addressLine1}</p>
              {order.address.addressLine2 && <p>{order.address.addressLine2}</p>}
              <p>
                {order.address.city}, {order.address.region}
                {order.address.postalCode ? ` ${order.address.postalCode}` : ""}
              </p>
              <p>{order.address.country}</p>
              <p className="mt-3 text-stone-500 text-sm">
                {order.address.email} · {order.address.phone}
              </p>
            </address>
          </div>

          <div>
            <h3 className="eyebrow text-stone-500">
              <span className="rule inline-block align-middle mr-3" />
              Delivery method
            </h3>
            <p className="mt-4 font-display text-lg tracking-tightest text-ink">
              {order.deliveryMethodName}
            </p>
            <p className="mt-2 text-stone-700 text-sm">
              {getDeliveryMethod(order.deliveryMethodId)?.eta}
            </p>
          </div>
        </section>

        <div className="mt-14 flex flex-col sm:flex-row gap-4">
          <Button asChild variant="primary" size="lg">
            <Link href="/shop">Continue Browsing</Link>
          </Button>
          <Button asChild variant="secondary" size="lg">
            <Link href="/contact">Speak With Us</Link>
          </Button>
        </div>
      </div>

      {/* Totals rail */}
      <aside
        aria-label="Order totals"
        className="lg:col-span-4 border border-ink/10 bg-stone-50 p-8 md:p-10"
      >
        <p className="eyebrow text-stone-500">
          <span className="rule inline-block align-middle mr-3" />
          Order summary
        </p>
        <dl className="mt-8 space-y-4">
          <div className="flex items-baseline justify-between">
            <dt className="text-stone-700">Subtotal</dt>
            <dd className="text-ink">{formatNaira(order.subtotal)}</dd>
          </div>
          <div className="flex items-baseline justify-between">
            <dt className="text-stone-700">Shipping</dt>
            <dd className="text-ink">
              {order.shipping === 0
                ? "Complimentary"
                : formatNaira(order.shipping)}
            </dd>
          </div>
          <div className="flex items-baseline justify-between border-t border-ink/10 pt-4 mt-4">
            <dt className="font-display text-lg tracking-tightest text-ink">
              Total
            </dt>
            <dd className="font-display text-2xl tracking-tight text-ink">
              {formatNaira(order.total)}
            </dd>
          </div>
          <div className="flex items-baseline justify-between border-t border-ink/10 pt-4 mt-4">
            <dt className="text-stone-700">Payment</dt>
            <dd className="text-xs uppercase tracking-widest text-gold-deep">
              Pending verification
            </dd>
          </div>
        </dl>

        <p className="mt-8 text-xs text-stone-500 leading-relaxed">
          Your payment is being verified by the atelier. You will receive a
          confirmation email once processed. Reference:{" "}
          <span className="text-ink select-all">{order.reference}</span>
        </p>
      </aside>
    </div>
  );
}

function ConfirmationNotFound({ reference }: { reference: string }) {
  return (
    <div className="mt-16 md:mt-20 text-center max-w-lg mx-auto">
      <p className="eyebrow">
        <span className="rule inline-block align-middle mr-3" />
        Order lookup
        <span className="rule inline-block align-middle ml-3" />
      </p>
      <h2 className="mt-8 font-display text-4xl md:text-5xl leading-[1.05] tracking-tightest">
        We could not find that order.
      </h2>
      <p className="mt-6 text-stone-700 leading-relaxed">
        {reference
          ? "The reference does not match anything on this device. If you have just completed a payment, please check the reference and try again — or write to the atelier and we will look it up."
          : "This confirmation page needs an order reference in the URL. If you have just paid and were redirected here, please contact the atelier."}
      </p>
      <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
        <Button asChild variant="primary" size="lg">
          <Link href="/shop">Return to Shop</Link>
        </Button>
        <Button asChild variant="secondary" size="lg">
          <Link href="/contact">Speak With Us</Link>
        </Button>
      </div>
    </div>
  );
}
