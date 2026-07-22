/**
 * Backend response envelopes.
 *
 * These types mirror the Spring Boot DTOs at
 *   com.eazicut.api.common.dto.ApiResponse
 *   com.eazicut.api.common.dto.PagedResponse
 *   com.eazicut.api.common.dto.ApiError
 *
 * Consumers should read these only inside the `src/lib/api/` layer.
 * UI components consume the adapted view models from `src/lib/api/adapters/`.
 */

/**
 * Uniform success envelope — used by single-item read endpoints
 * (`GET /products/{id}`, `GET /products/slug/{slug}`).
 */
export type ApiResponse<T> = {
  data: T;
  timestamp: string;
};

/**
 * Pagination envelope — used by every list endpoint (`GET /products`,
 * `/featured`, `/new-arrivals`, `/bestsellers`).
 *
 * Note: list endpoints return this shape at the top level; they are
 * NOT wrapped in an `ApiResponse`.
 */
export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};

/**
 * Field-level validation violation, present on 400 responses that
 * originated from Bean Validation on a request body.
 */
export type ApiFieldViolation = {
  field: string;
  message: string;
};

/**
 * Uniform error body — every non-2xx response from this API returns
 * an instance of this shape.
 */
export type ApiErrorBody = {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  details?: ApiFieldViolation[];
};
