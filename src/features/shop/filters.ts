import { collectionSlugs, type CollectionSlug } from "@/lib/assets";
import { productCategories, type ProductCategory, type Product } from "@/types/product";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export const sortModes = [
  "featured",
  "newest",
  "price-asc",
  "price-desc",
] as const;

export type SortMode = (typeof sortModes)[number];

export const sortLabels: Record<SortMode, string> = {
  featured: "Featured",
  newest: "Newest",
  "price-asc": "Price · Low to High",
  "price-desc": "Price · High to Low",
};

export type FilterState = {
  q: string;
  category: ProductCategory | null;
  collection: CollectionSlug | null;
  sort: SortMode;
};

export const emptyFilters: FilterState = {
  q: "",
  category: null,
  collection: null,
  sort: "featured",
};

// ---------------------------------------------------------------------------
// URL parsing — defensive against arbitrary user input
// ---------------------------------------------------------------------------

/**
 * Parse a `URLSearchParams`-like object into a validated `FilterState`.
 * Unknown values fall back to the safe default — never throws.
 */
export function parseFilters(
  params: URLSearchParams | ReadonlyURLSearchParams
): FilterState {
  const q = (params.get("q") ?? "").trim();

  const rawCategory = params.get("category");
  const category = (productCategories as readonly string[]).includes(
    rawCategory ?? ""
  )
    ? (rawCategory as ProductCategory)
    : null;

  const rawCollection = params.get("collection");
  const collection = (collectionSlugs as readonly string[]).includes(
    rawCollection ?? ""
  )
    ? (rawCollection as CollectionSlug)
    : null;

  const rawSort = params.get("sort");
  const sort = (sortModes as readonly string[]).includes(rawSort ?? "")
    ? (rawSort as SortMode)
    : "featured";

  return { q, category, collection, sort };
}

/**
 * Serialise a `FilterState` back to a query string. Omits defaults so
 * the "clean" URL is `/shop`, not `/shop?q=&category=&sort=featured`.
 */
export function serialiseFilters(state: FilterState): string {
  const params = new URLSearchParams();
  if (state.q) params.set("q", state.q);
  if (state.category) params.set("category", state.category);
  if (state.collection) params.set("collection", state.collection);
  if (state.sort !== "featured") params.set("sort", state.sort);
  const s = params.toString();
  return s ? `?${s}` : "";
}

/** True when any filter deviates from the defaults. */
export function hasActiveFilters(state: FilterState): boolean {
  return (
    state.q !== "" ||
    state.category !== null ||
    state.collection !== null ||
    state.sort !== "featured"
  );
}

// ---------------------------------------------------------------------------
// Filter + sort
// ---------------------------------------------------------------------------

const sorters: Record<SortMode, (a: Product, b: Product) => number> = {
  featured: () => 0, // preserve input order
  newest: (a, b) => b.createdAt.localeCompare(a.createdAt),
  "price-asc": (a, b) => a.price - b.price,
  "price-desc": (a, b) => b.price - a.price,
};

export function applyFilters(
  products: readonly Product[],
  state: FilterState
): Product[] {
  const q = state.q.toLowerCase();

  const filtered = products.filter((p) => {
    if (state.category && p.category !== state.category) return false;
    if (state.collection && p.collection !== state.collection) return false;
    if (q) {
      const hay = `${p.name} ${p.description} ${p.category}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });

  // Sort creates a new array so we never mutate the source data.
  return [...filtered].sort(sorters[state.sort]);
}

// ---------------------------------------------------------------------------
// Local re-export to avoid pulling `next/navigation` into this pure module.
// The type shape below matches what `useSearchParams` returns.
// ---------------------------------------------------------------------------

export interface ReadonlyURLSearchParams {
  get(key: string): string | null;
}
