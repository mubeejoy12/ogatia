import { apiBaseUrl } from "./config";
import {
  ApiAuthError,
  ApiClientError,
  ApiNetworkError,
  ApiNotFoundError,
  ApiValidationError,
} from "./errors";
import type { ApiErrorBody, ApiResponse } from "@/types/api/envelope";
import type {
  ApiAddCartItemRequest,
  ApiCart,
  ApiMergeCartRequest,
  ApiUpdateCartItemRequest,
} from "@/types/api/cart";

/**
 * Typed client for {@code /api/v1/cart}.
 *
 * <p>All endpoints require an authenticated caller — every method takes
 * an {@code accessToken}. Guest carts stay entirely client-side (see
 * {@code AuthContext} + {@code CartContext}); this module is the
 * <em>server-side</em> half of the split.
 *
 * <p>Deliberately separate from the generic {@code apiGet} — cart calls
 * need a bearer header and returns a well-typed {@link ApiCart}, and
 * we prefer a small dedicated file over pushing auth concerns into the
 * shared client.
 */

function cartUrl(path: string): string {
  const base = apiBaseUrl();
  const normalised = path.startsWith("/") ? path : `/${path}`;
  return `${base}${normalised}`;
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
  url: string,
): ApiClientError {
  const message = body?.message ?? `Request to ${url} returned ${status}.`;
  if (status === 404) return new ApiNotFoundError(message, body);
  if (status === 400) return new ApiValidationError(message, body);
  if (status === 401 || status === 403)
    return new ApiAuthError(message, status as 401 | 403, body);
  return new ApiClientError(message, status, body);
}

type Method = "GET" | "POST" | "PATCH" | "DELETE";

async function request<T>(
  method: Method,
  path: string,
  accessToken: string | null,
  body?: unknown,
): Promise<T> {
  const url = cartUrl(path);
  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "include",
      cache: "no-store",
    });
  } catch (cause) {
    throw new ApiNetworkError(`Network request to ${url} failed.`, cause);
  }

  if (!response.ok) {
    const err = await readErrorBody(response);
    throw errorForStatus(response.status, err, url);
  }
  if (response.status === 204) return undefined as T;
  try {
    return (await response.json()) as T;
  } catch (cause) {
    throw new ApiNetworkError(`Failed to parse JSON from ${url}.`, cause);
  }
}

/** Fetch the caller's cart. Lazy-creates an empty one on first call. */
export async function getCart(accessToken: string | null): Promise<ApiCart> {
  const envelope = await request<ApiResponse<ApiCart>>("GET", "/cart", accessToken);
  return envelope.data;
}

/** Add a line or increment an existing matching one. */
export async function addCartItem(
  accessToken: string | null,
  req: ApiAddCartItemRequest,
): Promise<ApiCart> {
  const envelope = await request<ApiResponse<ApiCart>>("POST", "/cart/items", accessToken, req);
  return envelope.data;
}

/** Set a line's absolute quantity (never a delta). */
export async function updateCartItem(
  accessToken: string | null,
  itemId: string,
  req: ApiUpdateCartItemRequest,
): Promise<ApiCart> {
  const envelope = await request<ApiResponse<ApiCart>>(
    "PATCH",
    `/cart/items/${encodeURIComponent(itemId)}`,
    accessToken,
    req,
  );
  return envelope.data;
}

/** Remove a single line. */
export async function removeCartItem(
  accessToken: string | null,
  itemId: string,
): Promise<ApiCart> {
  const envelope = await request<ApiResponse<ApiCart>>(
    "DELETE",
    `/cart/items/${encodeURIComponent(itemId)}`,
    accessToken,
  );
  return envelope.data;
}

/** Empty every line. */
export async function clearCart(accessToken: string | null): Promise<ApiCart> {
  const envelope = await request<ApiResponse<ApiCart>>("DELETE", "/cart", accessToken);
  return envelope.data;
}

/**
 * One-shot merge of a guest (localStorage) cart into the caller's
 * server cart. Called by the frontend immediately after login. Never
 * fails hard for a bad line — every skip becomes an entry in
 * {@code cart.issues[]}.
 */
export async function mergeCart(
  accessToken: string | null,
  req: ApiMergeCartRequest,
): Promise<ApiCart> {
  const envelope = await request<ApiResponse<ApiCart>>("POST", "/cart/merge", accessToken, req);
  return envelope.data;
}
