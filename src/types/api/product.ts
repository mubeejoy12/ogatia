/**
 * Backend Product API types.
 *
 * Types mirror the Spring Boot DTOs at
 *   com.eazicut.api.products.dto.ProductResponse
 *   com.eazicut.api.products.dto.ProductImageDto
 *   com.eazicut.api.products.dto.ProductFilterCriteria
 *   com.eazicut.api.categories.dto.CategorySummary
 *   com.eazicut.api.collections.dto.CollectionSummary
 *   com.eazicut.api.products.entity.ProductStatus
 *
 * Wire values only — never displayed directly. UI components consume the
 * adapted view models from `src/lib/api/adapters/`.
 */

export type ApiProductStatus =
  | "DRAFT"
  | "ACTIVE"
  | "INACTIVE"
  | "OUT_OF_STOCK"
  | "ARCHIVED";

export type ApiCategorySummary = {
  id: string;
  name: string;
  slug: string;
};

export type ApiCollectionSummary = {
  id: string;
  name: string;
  slug: string;
};

export type ApiProductImage = {
  id: string;
  url: string;
  alt: string | null;
  sortOrder: number;
  primary: boolean;
};

/**
 * The full backend Product response.
 *
 * `price` and `discountPrice` are Java `BigDecimal` on the server, which
 * Jackson serialises as JSON number by default (Spring Boot's config in
 * this project accepts the default). Consumers must not use these in
 * floating-point arithmetic beyond display formatting.
 *
 * `category` and `collection` are nullable — a product can be uncategorised
 * (though the storefront should treat that as a data anomaly).
 */
export type ApiProductResponse = {
  id: string;
  name: string;
  slug: string;
  shortDescription: string;
  fullDescription: string;
  sku: string;
  brand: string;
  price: number;
  discountPrice: number | null;
  currency: string;
  status: ApiProductStatus;
  stockQuantity: number;
  featured: boolean;
  newArrival: boolean;
  bestseller: boolean;
  fabricType: string | null;
  color: string | null;
  availableSizes: string[];
  tags: string[];
  images: ApiProductImage[];
  category: ApiCategorySummary | null;
  collection: ApiCollectionSummary | null;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Query / filter inputs
// ---------------------------------------------------------------------------

/**
 * Sort mode string accepted by Spring's `Pageable` on the list endpoint.
 * Format is `<field>,<asc|desc>` — any sortable field on `Product` works;
 * these are the ones the storefront uses.
 */
export type ApiProductSort =
  | "createdAt,desc"
  | "createdAt,asc"
  | "price,asc"
  | "price,desc"
  | "name,asc"
  | "name,desc";

/**
 * Query parameters accepted by `GET /api/v1/products`.
 *
 * Every field is optional. Pagination fields (`page`, `size`) and `sort`
 * are Spring conventions; the remaining fields correspond 1:1 to the
 * backend's `ProductFilterCriteria`.
 */
export type ApiProductFilter = {
  page?: number;
  size?: number;
  sort?: ApiProductSort;

  search?: string;
  /** Category slug — exact match. */
  category?: string;
  /** Collection slug — exact match. */
  collection?: string;
  minPrice?: number;
  maxPrice?: number;
  featured?: boolean;
  newArrival?: boolean;
  bestseller?: boolean;
  /** Computed on the server: `status = ACTIVE AND stockQuantity > 0`. */
  available?: boolean;
  status?: ApiProductStatus;
};
