"use client";

import Image from "next/image";
import { formatNaira } from "@/lib/format";
import type { CartLine } from "@/features/cart/types";
import type { DeliveryMethod } from "./deliveryMethods";

/**
 * The order summary rail on the checkout page.
 *
 * Shows every line in the bag (compressed — image + name + size + qty +
 * line total), then subtotal, shipping (or "Select delivery"), and grand
 * total. Sticky on desktop so it stays alongside the form as the user
 * scrolls through address entry.
 */
export function OrderReview({
  lines,
  subtotal,
  method,
}: {
  lines: readonly CartLine[];
  subtotal: number;
  method: DeliveryMethod | null;
}) {
  const shipping = method?.price ?? 0;
  const total = subtotal + shipping;

  return (
    <div className="border border-ink/10 bg-stone-50 p-8 md:p-10">
      <p className="eyebrow text-stone-500">
        <span className="rule inline-block align-middle mr-3" />
        Order review
      </p>

      <ul className="mt-6 divide-y divide-ink/10">
        {lines.map((line) => (
          <li key={line.id} className="flex gap-4 py-4">
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
              <p className="font-display text-base tracking-tightest leading-tight truncate">
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

      <dl className="mt-8 space-y-3 border-t border-ink/10 pt-6 text-sm">
        <div className="flex items-baseline justify-between">
          <dt className="text-stone-700">Subtotal</dt>
          <dd className="text-ink">{formatNaira(subtotal)}</dd>
        </div>
        <div className="flex items-baseline justify-between">
          <dt className="text-stone-700">Shipping</dt>
          <dd className="text-ink">
            {method
              ? shipping === 0
                ? "Complimentary"
                : formatNaira(shipping)
              : (
                <span className="text-stone-500 italic text-xs">
                  Select delivery
                </span>
              )}
          </dd>
        </div>
        <div className="flex items-baseline justify-between border-t border-ink/10 pt-4 mt-4">
          <dt className="font-display text-lg tracking-tightest text-ink">
            Total
          </dt>
          <dd className="font-display text-2xl tracking-tight text-ink">
            {formatNaira(total)}
          </dd>
        </div>
      </dl>

      <p className="mt-6 text-xs text-stone-500 leading-relaxed">
        Prices are in Nigerian Naira. Any applicable international duties are
        payable on arrival.
      </p>
    </div>
  );
}
