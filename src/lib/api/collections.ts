import { apiGet, type ApiGetOptions } from "./client";
import type { ApiResponse, PagedResponse } from "@/types/api/envelope";
import type { ApiCollectionResponse } from "@/types/api/collection";

/**
 * Typed read surface for the {@code /api/v1/collections} endpoints.
 *
 * <p>Direct parallel of {@code categories.ts}. Backend contract:
 * default sort {@code name ASC}, default server page size 50 — six
 * seeded rows fit in one round-trip.
 */

const COLLECTIONS_PATH = "/collections";

export function fetchCollections(
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<PagedResponse<ApiCollectionResponse>> {
  return apiGet<PagedResponse<ApiCollectionResponse>>(COLLECTIONS_PATH, {
    cache: options?.cache,
    revalidate: options?.revalidate,
    signal: options?.signal,
  });
}

/**
 * Fetch a single collection by slug. Throws {@code ApiNotFoundError}
 * when the slug is unknown.
 */
export async function fetchCollectionBySlug(
  slug: string,
  options?: Pick<ApiGetOptions, "cache" | "revalidate" | "signal">
): Promise<ApiCollectionResponse> {
  const envelope = await apiGet<ApiResponse<ApiCollectionResponse>>(
    `${COLLECTIONS_PATH}/slug/${encodeURIComponent(slug)}`,
    options
  );
  return envelope.data;
}
