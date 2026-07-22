import type { ApiProductFilter, ApiProductSort } from "@/types/api/product";
import type { FilterState, SortMode } from "./filters";
import { parseFilters, sortModes, type ReadonlyURLSearchParams } from "./filters";
import { collections } from "@/lib/data/collections";
import { productCategories } from "@/types/product";
import { collectionSlugs } from "@/lib/assets";

/**
 * Server-side page size for /shop. Keep in sync with the backend's
 * `spring.data.web.pageable.max-page-size` (currently 100). 24 is a
 * comfortable rhythm for a 3-column grid (8 rows) and keeps first-paint
 * payload lean.
 */
export const DEFAULT_PAGE_SIZE = 24;

/**
 * The raw shape Next.js hands to a server page for `searchParams` — every
 * value is either a string, an array of strings, or undefined.
 */
export type RawSearchParams = Record<string, string | string[] | undefined>;

/**
 * Adapt Next's server-side `searchParams` object into the same
 * `URLSearchParams`-compatible reader that `parseFilters` already
 * consumes on the client. Ensures server + client agree on what the URL
 * currently means.
 */
export function readableParams(raw: RawSearchParams): ReadonlyURLSearchParams {
  return {
    get(key: string): string | null {
      const value = raw[key];
      if (value === undefined) return null;
      if (Array.isArray(value)) return value[0] ?? null;
      return value;
    },
  };
}

/**
 * Parse the raw server-supplied `searchParams` into the display-time
 * {@link FilterState} the toolbar and grid render from.
 */
export function readFilterState(raw: RawSearchParams): FilterState {
  return parseFilters(readableParams(raw));
}

/**
 * Read the current 1-indexed page number the customer is viewing.
 *
 * The URL uses 1-indexed pages (`?page=2` = second page) — friendlier to
 * humans and to search engines than 0-indexed. Spring's `Pageable` is
 * 0-indexed; we translate here.
 */
export function readPageIndex(raw: RawSearchParams): number {
  const value = raw.page;
  const first = Array.isArray(value) ? value[0] : value;
  const parsed = first ? Number(first) : 1;
  if (!Number.isFinite(parsed) || parsed < 1) return 1;
  return Math.floor(parsed);
}

/**
 * Translate a display-time {@link FilterState} into the backend's
 * {@link ApiProductFilter}. Pagination (`page`, `size`) is added
 * separately by the server component.
 *
 * Every axis maps 1:1 — the frontend and backend share vocabulary for
 * category slugs, collection slugs, and product status.
 */
export function toApiFilter(state: FilterState): ApiProductFilter {
  return {
    search: state.q || undefined,
    category: state.category && CATEGORY_SLUG.get(state.category)
      ? CATEGORY_SLUG.get(state.category)!
      : undefined,
    collection: state.collection ?? undefined,
    sort: toApiSort(state.sort),
  };
}

/**
 * The frontend Sort modes map onto Spring's `sort=<field>,<direction>`
 * syntax. `featured` — the default in the frontend — has no backend
 * counterpart; we ask the backend for the default sort (`createdAt,desc`)
 * and treat the union as "backend default".
 */
export function toApiSort(mode: SortMode): ApiProductSort | undefined {
  switch (mode) {
    case "newest":
      return "createdAt,desc";
    case "price-asc":
      return "price,asc";
    case "price-desc":
      return "price,desc";
    case "featured":
    default:
      return undefined;
  }
}

// ---------------------------------------------------------------------------
// Category name → slug lookup
//
// The backend's `?category=` param accepts a slug ("suits"), but the
// frontend's ProductCategory union carries the display name ("Suits").
// We derive the slug from the display name using lower-kebab-case.
// A real Category module (later ticket) will publish an authoritative
// mapping; the derivation here is safe because the union is closed.
// ---------------------------------------------------------------------------

const CATEGORY_SLUG = new Map<string, string>(
  productCategories.map((name) => [name, name.toLowerCase().replace(/\s+/g, "-")])
);

// Re-export so consumers building storefront links can validate slugs.
export const KNOWN_COLLECTION_SLUGS: readonly string[] = collectionSlugs;
export const KNOWN_SORT_MODES: readonly SortMode[] = sortModes;
// Placeholder use — silences the unused-import lint in this pure module.
export const KNOWN_COLLECTION_NAMES = collections.map((c) => c.name);
