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
  ApiLoginRequest,
  ApiLoginResponse,
  ApiRegisterRequest,
  ApiUserResponse,
} from "@/types/api/auth";

/**
 * Typed client for the {@code /api/v1/auth} endpoints.
 *
 * <p>Deliberately separate from {@code src/lib/api/client.ts}'s
 * {@code apiGet} because every call here needs {@code credentials:
 * "include"} — otherwise the browser won't send or store the
 * HttpOnly refresh cookie. Rather than bake that switch into the
 * generic client (and open the possibility of accidentally sending
 * the refresh cookie on unrelated {@code /products} calls), we
 * keep a small POST-shaped helper here that always sets it.
 *
 * <p>Every function is intended for CLIENT-side use — the
 * {@code AuthContext} calls them from a "use client" boundary. SSR
 * doesn't handle sessions in B004; a later ticket can add
 * server-side forwarding of cookies for authenticated pages.
 */

function authUrl(path: string): string {
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

async function postJson<T>(
  path: string,
  body: unknown,
  accessToken?: string | null,
): Promise<T> {
  const url = authUrl(path);
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: "POST",
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "include", // send + store the HttpOnly refresh cookie
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

async function getWithAuth<T>(
  path: string,
  accessToken: string | null,
): Promise<T> {
  const url = authUrl(path);
  const headers: Record<string, string> = { Accept: "application/json" };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: "GET",
      headers,
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

/** Create a new CUSTOMER account. Returns the user (no tokens — caller must log in). */
export async function register(req: ApiRegisterRequest): Promise<ApiUserResponse> {
  const envelope = await postJson<ApiResponse<ApiUserResponse>>("/auth/register", req);
  return envelope.data;
}

/** Verify credentials, receive the access token, and the refresh cookie is stored automatically. */
export async function login(req: ApiLoginRequest): Promise<ApiLoginResponse> {
  const envelope = await postJson<ApiResponse<ApiLoginResponse>>("/auth/login", req);
  return envelope.data;
}

/**
 * Rotate the refresh cookie and receive a fresh access token. No
 * body — the cookie is the credential. Throws {@code ApiAuthError}
 * (401) if the cookie is missing, revoked, or expired.
 */
export async function refresh(): Promise<ApiLoginResponse> {
  const envelope = await postJson<ApiResponse<ApiLoginResponse>>("/auth/refresh", undefined);
  return envelope.data;
}

/**
 * Revoke the refresh cookie server-side and clear it in the browser.
 * Idempotent — safe to call from a logout button even when no
 * session exists.
 */
export async function logout(): Promise<void> {
  await postJson<void>("/auth/logout", undefined);
}

/** Return the authenticated user. 401 if the access token is missing/expired. */
export async function me(accessToken: string | null): Promise<ApiUserResponse> {
  const envelope = await getWithAuth<ApiResponse<ApiUserResponse>>("/auth/me", accessToken);
  return envelope.data;
}
