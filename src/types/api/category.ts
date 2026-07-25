/**
 * Mirror of the backend {@code CategoryResponse} record
 * ({@code com.eazicut.api.categories.dto.CategoryResponse}).
 *
 * <p>Kept in a dedicated file so the shop flow can import it without
 * pulling in the whole product envelope. Field names match the JSON on
 * the wire verbatim — do not rename without updating the backend at the
 * same time.
 */
export type ApiCategoryResponse = {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  createdAt: string; // ISO-8601 Instant
  updatedAt: string; // ISO-8601 Instant
};
