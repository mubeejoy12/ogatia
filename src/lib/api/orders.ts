import { apiBaseUrl } from "./config";
import {
  ApiAuthError,
  ApiClientError,
  ApiNetworkError,
  ApiNotFoundError,
  ApiValidationError,
} from "./errors";
import type {
  ApiErrorBody,
  ApiResponse,
  PagedResponse,
} from "@/types/api/envelope";
import type {
  ApiCreateOrderRequest,
  ApiOrder,
} from "@/types/api/orders";

/**
 * Typed client for {@code /api/v1/orders} — customer surface.
 *
 * <p>Every endpoint requires an authenticated caller. The
 * {@code accessToken} is passed in explicitly (pulled from
 * {@code AuthContext.getAccessToken()} at the call site) rather than
 * grabbed from a module-level singleton — keeps the client pure and
 * testable, and mirrors the {@code src/lib/api/cart.ts} pattern.
 *
 * <p>The idempotency-key generator lives in this module (see
 * {@link freshIdempotencyKey}) so the checkout page can either pass
 * one in or let the client mint one. Mint-once semantics on retry
 * are the caller's responsibility — the checkout view holds the key
 * in a ref so a double-click doesn't get two different keys and two
 * orders.
 */

function ordersUrl(path: string): string {
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

type Method = "GET" | "POST";

async function request<T>(
  method: Method,
  path: string,
  accessToken: string | null,
  extraHeaders?: Record<string, string>,
  body?: unknown,
): Promise<T> {
  const url = ordersUrl(path);
  const headers: Record<string, string> = {
    Accept: "application/json",
    ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
    ...(extraHeaders ?? {}),
  };
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

/**
 * Mint a fresh idempotency key for a checkout attempt. Uses
 * {@code crypto.randomUUID()} in the browser and Node 19+, falling
 * back to a millisecond+random string when unavailable (older Safari
 * still ships without it in edge cases). The caller should HOLD the
 * key in a ref between the initial POST and any retries: a fresh
 * key per user-initiated confirmation, the same key on network
 * retries.
 */
export function freshIdempotencyKey(): string {
  const globalCrypto =
    typeof globalThis !== "undefined"
      ? (globalThis as { crypto?: Crypto }).crypto
      : undefined;
  if (globalCrypto && typeof globalCrypto.randomUUID === "function") {
    return globalCrypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

/**
 * Create an order from the caller's server cart. The backend
 * atomically writes the order, decrements nothing (per D3), and
 * clears the cart. Same {@code (user, idempotencyKey)} replayed
 * returns the same order.
 */
export async function createOrder(
  accessToken: string | null,
  idempotencyKey: string,
  req: ApiCreateOrderRequest,
): Promise<ApiOrder> {
  const envelope = await request<ApiResponse<ApiOrder>>(
    "POST",
    "/orders",
    accessToken,
    { "Idempotency-Key": idempotencyKey },
    req,
  );
  return envelope.data;
}

/** List the caller's orders (paged, newest-first). */
export async function listOrders(
  accessToken: string | null,
  page = 0,
  size = 20,
): Promise<PagedResponse<ApiOrder>> {
  return request<PagedResponse<ApiOrder>>(
    "GET",
    `/orders?page=${page}&size=${size}`,
    accessToken,
  );
}

/** Fetch a single order by id (ownership-scoped: 404 if not caller's). */
export async function getOrderById(
  accessToken: string | null,
  id: string,
): Promise<ApiOrder> {
  const envelope = await request<ApiResponse<ApiOrder>>(
    "GET",
    `/orders/${encodeURIComponent(id)}`,
    accessToken,
  );
  return envelope.data;
}

/** Fetch a single order by its {@code EAZI-…} reference. */
export async function getOrderByReference(
  accessToken: string | null,
  reference: string,
): Promise<ApiOrder> {
  const envelope = await request<ApiResponse<ApiOrder>>(
    "GET",
    `/orders/reference/${encodeURIComponent(reference)}`,
    accessToken,
  );
  return envelope.data;
}
