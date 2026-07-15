"use client";

import Image from "next/image";
import Link from "next/link";
import { Minus, Plus, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatNaira } from "@/lib/format";
import type { CartLine } from "./types";

/**
 * A single line in the cart view.
 *
 * Composition:
 *  · Image (links to PDP)
 *  · Name (links to PDP) + category + collection
 *  · Size + quantity controls
 *  · Line total price
 *  · Remove button
 *
 * All controls are keyboard-reachable; the remove button carries a
 * descriptive aria-label so screen readers announce "Remove {name}, size {S}".
 */
export function CartLineItem({
  line,
  onIncrement,
  onDecrement,
  onRemove,
}: {
  line: CartLine;
  onIncrement: () => void;
  onDecrement: () => void;
  onRemove: () => void;
}) {
  const lineTotal = line.snapshot.price * line.quantity;
  const pdpHref = `/shop/${line.snapshot.slug}`;

  return (
    <article className="grid grid-cols-12 gap-4 sm:gap-6 py-8 border-b border-ink/10">
      {/* Image */}
      <Link
        href={pdpHref}
        aria-label={line.snapshot.name}
        className="col-span-4 sm:col-span-3 lg:col-span-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory"
      >
        <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
          <Image
            src={line.snapshot.image.src}
            alt={line.snapshot.image.alt}
            fill
            sizes="(min-width: 640px) 20vw, 40vw"
            className="object-cover"
          />
        </div>
      </Link>

      {/* Meta + controls */}
      <div className="col-span-8 sm:col-span-9 lg:col-span-10 flex flex-col justify-between gap-4">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="eyebrow text-stone-500">{line.snapshot.category}</p>
            <h3 className="mt-2 font-display text-xl md:text-2xl tracking-tightest leading-tight">
              <Link
                href={pdpHref}
                className="hover:text-gold-deep transition-colors"
              >
                {line.snapshot.name}
              </Link>
            </h3>
            <dl className="mt-3 text-xs text-stone-500 flex flex-wrap gap-x-6 gap-y-1">
              <div className="flex gap-2">
                <dt className="uppercase tracking-widest">Size</dt>
                <dd className="text-ink">{line.size}</dd>
              </div>
              <div className="flex gap-2">
                <dt className="uppercase tracking-widest">Unit</dt>
                <dd className="text-ink">{formatNaira(line.snapshot.price)}</dd>
              </div>
            </dl>
          </div>

          <p className="shrink-0 font-display text-lg md:text-xl tracking-tight text-ink whitespace-nowrap">
            {formatNaira(lineTotal)}
          </p>
        </div>

        <div className="flex items-center justify-between gap-4">
          <QuantityControl
            quantity={line.quantity}
            onDecrement={onDecrement}
            onIncrement={onIncrement}
            productName={line.snapshot.name}
            size={line.size}
          />
          <button
            type="button"
            onClick={onRemove}
            aria-label={`Remove ${line.snapshot.name}, size ${line.size}`}
            className="inline-flex items-center gap-2 text-xs uppercase tracking-widest text-stone-500 hover:text-ink transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory"
          >
            <X size={14} aria-hidden />
            Remove
          </button>
        </div>
      </div>
    </article>
  );
}

function QuantityControl({
  quantity,
  onDecrement,
  onIncrement,
  productName,
  size,
}: {
  quantity: number;
  onDecrement: () => void;
  onIncrement: () => void;
  productName: string;
  size: string;
}) {
  const btn =
    "w-9 h-9 grid place-items-center border border-ink/30 hover:border-ink hover:bg-ink hover:text-ivory transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory disabled:opacity-40 disabled:pointer-events-none";

  return (
    <div
      className="inline-flex items-stretch"
      role="group"
      aria-label={`Quantity for ${productName}, size ${size}`}
    >
      <button
        type="button"
        onClick={onDecrement}
        aria-label="Decrease quantity"
        className={cn(btn, "border-r-0")}
      >
        <Minus size={14} aria-hidden />
      </button>
      <span
        aria-live="polite"
        className="min-w-11 h-9 grid place-items-center px-3 border-y border-ink/30 font-display text-lg"
      >
        {quantity}
      </span>
      <button
        type="button"
        onClick={onIncrement}
        aria-label="Increase quantity"
        className={cn(btn, "border-l-0")}
      >
        <Plus size={14} aria-hidden />
      </button>
    </div>
  );
}
