/**
 * Resolve the backend API base URL.
 *
 * Read from `NEXT_PUBLIC_API_URL` at build/render time. Trailing slash
 * is trimmed so callers can pass `/products` (with a leading slash)
 * without doubling up.
 *
 * The `NEXT_PUBLIC_` prefix is required for the value to reach the
 * browser bundle. This URL points at the public read endpoints only;
 * write endpoints require admin auth and are not called from the
 * customer-facing site.
 */

const DEFAULT_BASE_URL = "http://localhost:8080/api/v1";

export function apiBaseUrl(): string {
  const raw = process.env.NEXT_PUBLIC_API_URL?.trim();
  const value = raw && raw.length > 0 ? raw : DEFAULT_BASE_URL;
  return value.replace(/\/+$/, "");
}
