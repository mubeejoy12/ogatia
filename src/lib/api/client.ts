import { apiBaseUrl } from "./config";
import {
  ApiAuthError,
  ApiClientError,
  ApiNetworkError,
  ApiNotFoundError,
  ApiValidationError,
} from "./errors";
import type { ApiErrorBody } from "@/types/api/envelope";

/**
 * A single-parameter primitive we can safely serialise into a query
 * string. Arrays are joined by comma (matches Spring's default
 * behaviour for `List<T>` parameters); `null` / `undefined` values
 * are dropped so callers can spread partial filter objects.
 */
type Primitive = string | number | boolean | null | undefined;
export type QueryValue = Primitive | Primitive[];
export type QueryParams = Record<string, QueryValue>;

export type ApiGetOptions = {
  /** Query string params — merged onto the URL. */
  searchParams?: QueryParams;
  /**
   * Next.js fetch cache directive. Defaults to `"no-store"` — every
   * request hits the backend, no ISR. Set `revalidate: 60` (etc.)
   * for opportunistic caching.
   */
  cache?: RequestCache;
  revalidate?: number | false;
  /** Optional AbortSignal for cancellation. */
  signal?: AbortSignal;
};

/**
 * Typed GET against the backend API.
 *
 * Handles:
 *  - query-string composition (drops null/undefined; comma-joins arrays)
 *  - Accept + Content-Type headers
 *  - Non-2xx → typed subclass of ApiClientError
 *  - Network / parse errors → ApiNetworkError
 *  - Empty 204 responses → returns undefined (typed as T at the caller's risk)
 */
export async function apiGet<T>(
  path: string,
  options: ApiGetOptions = {}
): Promise<T> {
  const url = buildUrl(path, options.searchParams);
  const init: RequestInit & { next?: { revalidate?: number | false } } = {
    method: "GET",
    headers: { Accept: "application/json" },
    signal: options.signal,
  };

  if (options.cache) {
    init.cache = options.cache;
  } else if (options.revalidate !== undefined) {
    init.next = { revalidate: options.revalidate };
  } else {
    init.cache = "no-store";
  }

  let response: Response;
  try {
    response = await fetch(url, init);
  } catch (cause) {
    throw new ApiNetworkError(`Network request to ${url} failed.`, cause);
  }

  if (!response.ok) {
    const body = await readErrorBody(response);
    throw errorForStatus(response.status, body, url);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  try {
    return (await response.json()) as T;
  } catch (cause) {
    throw new ApiNetworkError(`Failed to parse JSON from ${url}.`, cause);
  }
}

// ---------------------------------------------------------------------------
// Internals
// ---------------------------------------------------------------------------

function buildUrl(
  path: string,
  searchParams?: QueryParams
): string {
  const base = apiBaseUrl();
  const normalisedPath = path.startsWith("/") ? path : `/${path}`;
  const url = `${base}${normalisedPath}`;

  if (!searchParams) return url;
  const qs = toQueryString(searchParams);
  return qs ? `${url}?${qs}` : url;
}

function toQueryString(params: QueryParams): string {
  const usp = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined) continue;
    if (Array.isArray(value)) {
      const joined = value
        .filter((v): v is Exclude<Primitive, null | undefined> =>
          v !== null && v !== undefined
        )
        .map(String)
        .join(",");
      if (joined) usp.set(key, joined);
      continue;
    }
    if (typeof value === "string" && value.trim() === "") continue;
    usp.set(key, String(value));
  }
  return usp.toString();
}

async function readErrorBody(response: Response): Promise<ApiErrorBody | null> {
  try {
    const text = await response.text();
    if (!text) return null;
    return JSON.parse(text) as ApiErrorBody;
  } catch {
    return null;
  }
}

function errorForStatus(
  status: number,
  body: ApiErrorBody | null,
  url: string
): ApiClientError {
  const message = body?.message ?? `Request to ${url} returned ${status}.`;
  if (status === 404) return new ApiNotFoundError(message, body);
  if (status === 400) return new ApiValidationError(message, body);
  if (status === 401 || status === 403)
    return new ApiAuthError(message, status as 401 | 403, body);
  return new ApiClientError(message, status, body);
}
