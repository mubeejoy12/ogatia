import type { ApiErrorBody, ApiFieldViolation } from "@/types/api/envelope";

/**
 * Base error thrown by the API client on any non-2xx response, or on any
 * network / parse failure.
 *
 * Every subclass carries the parsed `ApiErrorBody` when the server
 * returned one; on network / parse failure `body` is `null`.
 *
 * UI code catches these and renders branded error / retry states. Raw
 * error bodies are never displayed to customers.
 */
export class ApiClientError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | null;

  constructor(message: string, status: number, body: ApiErrorBody | null) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.body = body;
  }
}

/** 404 — resource lookup returned nothing. */
export class ApiNotFoundError extends ApiClientError {
  constructor(message: string, body: ApiErrorBody | null) {
    super(message, 404, body);
    this.name = "ApiNotFoundError";
  }
}

/** 400 — request-shape or business validation failure. */
export class ApiValidationError extends ApiClientError {
  readonly violations: ApiFieldViolation[];

  constructor(message: string, body: ApiErrorBody | null) {
    super(message, 400, body);
    this.name = "ApiValidationError";
    this.violations = body?.details ?? [];
  }
}

/** 401 / 403 — auth required or denied. */
export class ApiAuthError extends ApiClientError {
  constructor(message: string, status: 401 | 403, body: ApiErrorBody | null) {
    super(message, status, body);
    this.name = "ApiAuthError";
  }
}

/**
 * Network layer failure — DNS, connection refused, TLS, JSON parse,
 * or any error not tied to a specific HTTP status.
 */
export class ApiNetworkError extends ApiClientError {
  constructor(message: string, cause?: unknown) {
    super(message, 0, null);
    this.name = "ApiNetworkError";
    if (cause instanceof Error) this.cause = cause;
  }
}
