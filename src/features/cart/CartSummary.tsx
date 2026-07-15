"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { formatNaira } from "@/lib/format";

/**
 * The order summary rail on the cart page.
 *
 * Subtotal shown; shipping + duties noted as calculated at checkout — the
 * standard luxury-ecommerce pattern. The "Proceed to Checkout" CTA routes
 * to /checkout, which will be built in the next milestone. Until then it
 * falls through to the branded 404 (honest, not a stub).
 */
export function CartSummary({
  subtotal,
  count,
}: {
  subtotal: number;
  count: number;
}) {
  return (
    <aside
      aria-label="Order summary"
      className="lg:sticky lg:top-28 border border-ink/10 bg-stone-50 p-8 md:p-10"
    >
      <h2 className="eyebrow text-stone-500">
        <span className="rule inline-block align-middle mr-3" />
        Order summary
      </h2>

      <dl className="mt-8 space-y-4">
        <div className="flex items-baseline justify-between">
          <dt className="text-stone-700">Subtotal</dt>
          <dd className="font-display text-2xl tracking-tight text-ink">
            {formatNaira(subtotal)}
          </dd>
        </div>
        <div className="flex items-baseline justify-between">
          <dt className="text-stone-700">Pieces</dt>
          <dd className="text-ink">{count}</dd>
        </div>
      </dl>

      <p className="mt-8 text-xs text-stone-500 leading-relaxed">
        Shipping and any applicable duties are calculated at checkout.
        Ready-to-wear ships worldwide from Lagos.
      </p>

      <div className="mt-8 flex flex-col gap-3">
        <Button asChild variant="primary" size="lg" className="w-full">
          <Link href="/checkout">Proceed to Checkout</Link>
        </Button>
        <Button asChild variant="ghost" size="lg" className="w-full">
          <Link href="/shop">Continue Shopping</Link>
        </Button>
      </div>
    </aside>
  );
}
