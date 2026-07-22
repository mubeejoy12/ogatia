import type {
  ApiPagedFetch,
} from "./types-internal";
import { apiGet, type ApiGetOptions, type QueryParams } from "./client";
import type { ApiResponse, PagedResponse } from "@/types/api/envelope";
import type {
  ApiProductFilter,
  ApiProductResponse,
} from "@/types/api/product";

/**
 * The public read surface of the Product API.
 *
 * These functions return backend types (`ApiProductResponse`, `PagedResponse`).
 * UI-facing code should call them through the adapter layer at
 * `src/lib/api/adapters/product.ts`, which normalises the shape into the
 * view-model type consumed by components.
 */

// ---------------------------------------------------------------------------
// List / filter
// ---------------------------------------------------------------------------

const PRODUCTS_PATH = "/products";

export function fetchProducts(
  filter: ApiProductFilter = {},
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<PagedResponse<ApiProductResponse>> {
  return apiGet<PagedResponse<ApiProductResponse>>(PRODUCTS_PATH, {
    searchParams: filterToParams(filter),
    ...options,
  });
}

// ---------------------------------------------------------------------------
// Curated storefront lists (backend-provided shortcuts)
// ---------------------------------------------------------------------------

export function fetchFeaturedProducts(
  filter: PagingOnly = {},
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<PagedResponse<ApiProductResponse>> {
  return apiGet<PagedResponse<ApiProductResponse>>(`${PRODUCTS_PATH}/featured`, {
    searchParams: pagingParams(filter),
    ...options,
  });
}

export function fetchNewArrivals(
  filter: PagingOnly = {},
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<PagedResponse<ApiProductResponse>> {
  return apiGet<PagedResponse<ApiProductResponse>>(`${PRODUCTS_PATH}/new-arrivals`, {
    searchParams: pagingParams(filter),
    ...options,
  });
}

export function fetchBestsellers(
  filter: PagingOnly = {},
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<PagedResponse<ApiProductResponse>> {
  return apiGet<PagedResponse<ApiProductResponse>>(`${PRODUCTS_PATH}/bestsellers`, {
    searchParams: pagingParams(filter),
    ...options,
  });
}

// ---------------------------------------------------------------------------
// Single-item reads (wrapped in ApiResponse)
// ---------------------------------------------------------------------------

export async function fetchProductBySlug(
  slug: string,
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<ApiProductResponse> {
  const envelope = await apiGet<ApiResponse<ApiProductResponse>>(
    `${PRODUCTS_PATH}/slug/${encodeURIComponent(slug)}`,
    options
  );
  return envelope.data;
}

export async function fetchProductById(
  id: string,
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<ApiProductResponse> {
  const envelope = await apiGet<ApiResponse<ApiProductResponse>>(
    `${PRODUCTS_PATH}/${encodeURIComponent(id)}`,
    options
  );
  return envelope.data;
}

// ---------------------------------------------------------------------------
// Filter → query-param mapping
// ---------------------------------------------------------------------------

type PagingOnly = Pick<ApiProductFilter, "page" | "size" | "sort">;

// Explicitly re-export the pattern so future callers can build search-param
// objects without touching internal shape.
export type { ApiPagedFetch };

function filterToParams(filter: ApiProductFilter): QueryParams {
  // The client's toQueryString drops null / undefined, so we can hand the
  // record over unchanged. Booleans serialise as "true" / "false" — which
  // is exactly what Spring's binding expects.
  return {
    page: filter.page,
    size: filter.size,
    sort: filter.sort,
    search: filter.search,
    category: filter.category,
    collection: filter.collection,
    minPrice: filter.minPrice,
    maxPrice: filter.maxPrice,
    featured: filter.featured,
    newArrival: filter.newArrival,
    bestseller: filter.bestseller,
    available: filter.available,
    status: filter.status,
  };
}

function pagingParams(filter: PagingOnly): QueryParams {
  return {
    page: filter.page,
    size: filter.size,
    sort: filter.sort,
  };
}
