import { apiGet, type ApiGetOptions } from "./client";
import type { ApiResponse, PagedResponse } from "@/types/api/envelope";
import type { ApiCategoryResponse } from "@/types/api/category";

/**
 * Typed read surface for the {@code /api/v1/categories} endpoints.
 *
 * <p>Reference data changes rarely (six curated rows on a dev boot), so
 * consumers can cache aggressively; the defaults here still honour
 * {@link ApiGetOptions} so the caller controls freshness explicitly.
 *
 * <p>Symmetric with {@link fetchProducts} — same envelope conventions,
 * same {@link ApiGetOptions}, same error mapping via {@code apiGet}.
 */

const CATEGORIES_PATH = "/categories";

/**
 * List categories, backend-sorted by {@code name ASC} by default (server
 * decides via {@code @PageableDefault(size = 50, sort = "name")}).
 * The default page size on the server side is 50, comfortably covering
 * the six seeded reference-data rows in one round-trip.
 */
export function fetchCategories(
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<PagedResponse<ApiCategoryResponse>> {
  return apiGet<PagedResponse<ApiCategoryResponse>>(CATEGORIES_PATH, {
    cache: options?.cache,
    revalidate: options?.revalidate,
    signal: options?.signal,
  });
}

/**
 * Fetch a single category by slug. Returns the {@code data} payload
 * unwrapped from the {@link ApiResponse} envelope so consumers work
 * with the plain response record.
 *
 * <p>Throws {@code ApiNotFoundError} when the slug is unknown — caller
 * should surface a branded not-found or coerce to a safe fallback.
 */
export async function fetchCategoryBySlug(
  slug: string,
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<ApiCategoryResponse> {
  const envelope = await apiGet<ApiResponse<ApiCategoryResponse>>(
    `${CATEGORIES_PATH}/slug/${encodeURIComponent(slug)}`,
    options
  );
  return envelope.data;
}
