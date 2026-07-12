"use client";

import { useCallback, useMemo } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ProductCard } from "@/components/shop/ProductCard";
import type { Product } from "@/types/product";
import { ShopToolbar } from "./ShopToolbar";
import { ShopEmptyState } from "./ShopEmptyState";
import {
  applyFilters,
  emptyFilters,
  hasActiveFilters,
  parseFilters,
  serialiseFilters,
  type FilterState,
} from "./filters";

/**
 * The interactive shopping surface.
 *
 * Reads the URL as the single source of truth for filter state, so:
 *   · Every filter combination is deep-linkable and shareable
 *   · Back / forward buttons behave predictably
 *   · The incoming `?collection={slug}` from Ticket 002.1's detail-page
 *     CTAs applies automatically on first render
 *
 * Uses `router.replace` (not `push`) so filter selections don't spawn
 * history entries — users don't expect Back to undo each dropdown.
 */
export function ShopBrowser({ products }: { products: readonly Product[] }) {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const state = useMemo(() => parseFilters(searchParams), [searchParams]);
  const filtered = useMemo(
    () => applyFilters(products, state),
    [products, state]
  );
  const isFiltered = hasActiveFilters(state);

  const setState = useCallback(
    (next: FilterState) => {
      router.replace(`${pathname}${serialiseFilters(next)}`, { scroll: false });
    },
    [router, pathname]
  );

  const clear = useCallback(() => setState(emptyFilters), [setState]);

  return (
    <>
      <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-3">
        <p className="eyebrow text-stone-500">
          <span className="rule inline-block align-middle mr-3" />
          {filtered.length === products.length
            ? `${products.length} pieces`
            : `${filtered.length} of ${products.length} pieces`}
        </p>
        {isFiltered && (
          <button
            type="button"
            onClick={clear}
            className="self-start sm:self-auto uppercase tracking-widest text-[11px] text-stone-700 border-b border-stone-400 hover:border-ink hover:text-ink pb-1 transition-colors"
          >
            Clear all filters
          </button>
        )}
      </div>

      <ShopToolbar state={state} onChange={setState} />

      {filtered.length === 0 ? (
        <ShopEmptyState onClear={clear} />
      ) : (
        <div className="mt-14 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-16">
          {filtered.map((product, i) => (
            <ProductCard
              key={product.slug}
              product={product}
              priority={i < 3}
            />
          ))}
        </div>
      )}
    </>
  );
}
