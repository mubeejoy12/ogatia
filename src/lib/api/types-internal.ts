/**
 * Internal type helpers for the API client.
 *
 * Kept in a small module so `src/lib/api/products.ts` can re-export names
 * consumers might want without leaking implementation details.
 */

import type { PagedResponse } from "@/types/api/envelope";

/**
 * Convenience alias for any function that returns a paginated backend
 * page. Used by callers that compose paginated data (e.g. related-products
 * strips that mix multiple fetch calls).
 */
export type ApiPagedFetch<T> = () => Promise<PagedResponse<T>>;
