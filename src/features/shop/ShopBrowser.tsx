"use client";

import { useCallback, useMemo } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ProductCard } from "@/components/shop/ProductCard";
import type { Product } from "@/types/product";
import { ShopToolbar } from "./ShopToolbar";
import { ShopEmptyState } from "./ShopEmptyState";
import { ShopPagination } from "./ShopPagination";
import {
  emptyFilters,
  hasActiveFilters,
  parseFilters,
  serialiseFilters,
  type FilterState,
} from "./filters";

/**
 * Paginated view of products, as delivered by the server component after
 * calling the backend and running the adapter. Mirrors the backend's
 * `PagedResponse<T>` (via `mapApiProductPage`) but with 1-indexed
 * `currentPage` for customer-facing URLs.
 */
export type ProductPage = {
  items: Product[];
  /** 1-indexed for the URL/UI. */
  currentPage: number;
  totalPages: number;
  totalElements: number;
};

/**
 * The interactive shop surface.
 *
 * The URL is the single source of truth for filter, sort and page state.
 * The server component parses the URL, fetches the matching page from
 * the backend, and passes the result as {@link ProductPage}. This
 * component:
 *
 *  · displays the current filter state in the toolbar
 *  · renders the pre-filtered grid
 *  · mounts pagination controls
 *  · on any toolbar change, updates the URL via `router.replace` (no
 *    history entries — Back should not undo dropdown selections);
 *    the server component re-runs the fetch and re-supplies the props
 */
export function ShopBrowser({ page }: { page: ProductPage }) {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const state = useMemo(() => parseFilters(searchParams), [searchParams]);
  const isFiltered = hasActiveFilters(state);

  const setState = useCallback(
    (next: FilterState) => {
      // Any filter change collapses back to page 1 — the current page
      // number is meaningless against a different result set.
      const params = new URLSearchParams(serialiseFilters(next).replace(/^\?/, ""));
      params.delete("page");
      const qs = params.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [router, pathname]
  );

  const clear = useCallback(() => setState(emptyFilters), [setState]);

  return (
    <>
      <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-3">
        <p className="eyebrow text-stone-500">
          <span className="rule inline-block align-middle mr-3" />
          {page.totalElements === 1
            ? "1 piece"
            : `${page.totalElements} pieces`}
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

      {page.items.length === 0 ? (
        <ShopEmptyState onClear={clear} />
      ) : (
        <>
          <div className="mt-14 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-16">
            {page.items.map((product, i) => (
              <ProductCard
                key={product.slug}
                product={product}
                priority={i < 3}
              />
            ))}
          </div>

          <ShopPagination page={page.currentPage} totalPages={page.totalPages} />
        </>
      )}
    </>
  );
}
