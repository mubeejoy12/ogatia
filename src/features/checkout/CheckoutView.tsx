"use client";

import { useCallback, useEffect, useState } from "react";
import { useCart } from "@/features/cart/CartContext";
import { AddressForm } from "./AddressForm";
import { CheckoutEmpty } from "./CheckoutEmpty";
import type { ShippingAddress } from "./types";

/**
 * The checkout orchestrator.
 *
 * Stage 1 responsibilities:
 *  · Hydration-aware — renders a skeleton until the cart has read
 *    from localStorage (avoids a false "empty" flash for returning
 *    customers with a persisted bag).
 *  · Empty-cart guard — routes visitors back to shop with a branded
 *    message if the bag is empty.
 *  · Mounts AddressForm; keeps the current address + validity in
 *    local state, ready for Stage 2 to read.
 */
export function CheckoutView() {
  const cart = useCart();
  const [hydrated, setHydrated] = useState(false);
  const [, setAddress] = useState<ShippingAddress | null>(null);
  const [, setIsValid] = useState(false);

  useEffect(() => setHydrated(true), []);

  const handleAddressChange = useCallback(
    (next: ShippingAddress, valid: boolean) => {
      setAddress(next);
      setIsValid(valid);
    },
    []
  );

  if (!hydrated) {
    return <div aria-hidden className="min-h-[50vh]" />;
  }

  if (cart.lines.length === 0) {
    return <CheckoutEmpty />;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-start">
      <div className="lg:col-span-8">
        <section aria-labelledby="ship-heading">
          <h2
            id="ship-heading"
            className="font-display text-2xl md:text-3xl tracking-tightest"
          >
            Shipping details
          </h2>
          <p className="mt-3 text-stone-700 max-w-lg leading-relaxed">
            Where should we deliver the commission? A member of the atelier
            team will confirm every detail before shipping.
          </p>

          <div className="mt-10">
            <AddressForm onChange={handleAddressChange} />
          </div>
        </section>
      </div>

      <aside
        aria-label="Order summary"
        className="lg:col-span-4 lg:sticky lg:top-28 border border-ink/10 bg-stone-50 p-8"
      >
        <p className="eyebrow text-stone-500">
          <span className="rule inline-block align-middle mr-3" />
          Order summary
        </p>
        <p className="mt-6 text-stone-700 text-sm">
          {cart.count === 1 ? "1 piece" : `${cart.count} pieces`} in the bag.
        </p>
        <p className="mt-6 text-stone-500 text-xs leading-relaxed">
          Delivery method and total will be calculated in the next step.
        </p>
      </aside>
    </div>
  );
}
