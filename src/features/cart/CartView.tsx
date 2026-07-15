"use client";

import { useEffect, useState } from "react";
import { useCart } from "./CartContext";
import { CartLineItem } from "./CartLineItem";
import { CartSummary } from "./CartSummary";
import { CartEmpty } from "./CartEmpty";

/**
 * The interactive cart surface.
 *
 * Hydration-aware: on first render we don't yet know whether the visitor
 * has a persisted cart (that's read from localStorage in an effect). We
 * render a skeleton for one paint to avoid flashing "empty" then filled.
 * The skeleton has the same section height as either branch so there's
 * no layout shift.
 */
export function CartView() {
  const cart = useCart();
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => setHydrated(true), []);

  if (!hydrated) {
    return (
      <div aria-hidden className="min-h-[50vh]" />
    );
  }

  if (cart.lines.length === 0) {
    return <CartEmpty />;
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-start">
      <div className="lg:col-span-8">
        <p className="eyebrow text-stone-500">
          <span className="rule inline-block align-middle mr-3" />
          {cart.count === 1 ? "1 piece" : `${cart.count} pieces`}
        </p>
        <div className="mt-6 border-t border-ink/10">
          {cart.lines.map((line) => (
            <CartLineItem
              key={line.id}
              line={line}
              onIncrement={() => cart.setQuantity(line.id, line.quantity + 1)}
              onDecrement={() => cart.setQuantity(line.id, line.quantity - 1)}
              onRemove={() => cart.remove(line.id)}
            />
          ))}
        </div>
      </div>

      <div className="lg:col-span-4">
        <CartSummary subtotal={cart.subtotal} count={cart.count} />
      </div>
    </div>
  );
}
