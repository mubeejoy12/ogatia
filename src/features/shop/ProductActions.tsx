"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Check, Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useCart } from "@/features/cart/CartContext";
import { SizeSelector } from "./SizeSelector";
import type { Product } from "@/types/product";

/**
 * The Add-to-Cart / Save action strip on the product detail page.
 *
 * The Add-to-Cart button is fully wired to the cart store introduced in
 * this ticket: on success, the piece persists to localStorage and the
 * user sees a subtle "Added to bag" confirmation.
 *
 * The Save (wishlist) button is scaffolded — it flips a local visual
 * state so the user knows the intent registered. When the accounts +
 * wishlist ticket lands, this callback will move to server-side
 * persistence with no markup change.
 */
export function ProductActions({ product }: { product: Product }) {
  const cart = useCart();

  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState(false);
  const [saved, setSaved] = useState(false);
  const confirmTimer = useRef<number | null>(null);

  // Clear any pending confirmation timer on unmount.
  useEffect(() => {
    return () => {
      if (confirmTimer.current) window.clearTimeout(confirmTimer.current);
    };
  }, []);

  const handleAdd = useCallback(() => {
    if (!product.available) return;
    if (!selectedSize) {
      setError("Choose a size before adding to the bag.");
      return;
    }
    setError(null);
    cart.add(
      {
        slug: product.slug,
        name: product.name,
        price: product.price,
        image: product.image,
        collection: product.collection,
        category: product.category,
      },
      selectedSize,
      1
    );

    setConfirmed(true);
    if (confirmTimer.current) window.clearTimeout(confirmTimer.current);
    confirmTimer.current = window.setTimeout(() => setConfirmed(false), 2400);
  }, [cart, product, selectedSize]);

  const handleSave = useCallback(() => {
    // Placeholder for the accounts + wishlist ticket. Local visual only.
    setSaved((s) => !s);
  }, []);

  return (
    <div className="mt-10">
      {product.available && (
        <SizeSelector
          sizes={product.sizes}
          selected={selectedSize}
          onSelect={(s) => {
            setSelectedSize(s);
            setError(null);
          }}
        />
      )}

      <div className="mt-8 flex flex-col sm:flex-row gap-3">
        {product.available ? (
          <Button
            type="button"
            variant="primary"
            size="lg"
            onClick={handleAdd}
            className="sm:flex-1"
            aria-describedby={error ? "add-error" : undefined}
          >
            Add to Bag
          </Button>
        ) : (
          <Button asChild variant="primary" size="lg" className="sm:flex-1">
            <Link href={`/contact?product=${product.slug}`}>Enquire</Link>
          </Button>
        )}

        <Button
          type="button"
          variant="secondary"
          size="lg"
          onClick={handleSave}
          aria-pressed={saved}
          className={cn("gap-3", saved && "border-gold text-gold-deep")}
        >
          <Heart
            size={16}
            className={cn(saved ? "fill-gold-deep" : "fill-none")}
            aria-hidden
          />
          {saved ? "Saved" : "Save"}
        </Button>
      </div>

      {/* Inline live region — announces error / confirmation to AT users */}
      <div aria-live="polite" className="min-h-6 mt-4">
        {error && (
          <p id="add-error" className="text-xs text-gold-deep">
            {error}
          </p>
        )}
        {confirmed && (
          <p className="flex items-center gap-2 text-xs uppercase tracking-widest text-ink">
            <Check size={14} className="text-gold" aria-hidden />
            Added to bag · {selectedSize}
          </p>
        )}
      </div>
    </div>
  );
}
