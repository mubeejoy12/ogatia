"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ShoppingBag } from "lucide-react";
import { cn } from "@/lib/utils";
import { useCart } from "./CartContext";

/**
 * The cart affordance in the site header.
 *
 * Hydration-safe: renders a neutral bag icon on the server + before
 * useEffect fires, then reveals the count chip once the cart has
 * hydrated from localStorage. This avoids a server/client mismatch
 * and also prevents a "0 → 3" flash for returning customers.
 *
 * The count chip uses aria-live so a screen reader announces changes
 * when items are added (e.g. right after "Add to Bag" on a PDP).
 */
export function CartBadge({ className }: { className?: string }) {
  const cart = useCart();
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => setHydrated(true), []);

  const count = hydrated ? cart.count : 0;
  const label = count > 0 ? `The Bag, ${count} pieces` : "The Bag";

  return (
    <Link
      href="/cart"
      aria-label={label}
      className={cn(
        "relative inline-flex items-center justify-center w-10 h-10 -mr-2 text-ink",
        "focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory",
        "hover:text-gold-deep transition-colors",
        className
      )}
    >
      <ShoppingBag size={20} aria-hidden />

      {count > 0 && (
        <span
          aria-hidden
          className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 grid place-items-center rounded-full bg-gold text-ivory text-[10px] font-sans font-medium leading-none"
        >
          {count > 99 ? "99+" : count}
        </span>
      )}

      {/* Screen-reader-only live announcement for count changes */}
      <span aria-live="polite" className="sr-only">
        {count > 0 ? `${count} pieces in the bag` : ""}
      </span>
    </Link>
  );
}
