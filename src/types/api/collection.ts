/**
 * Mirror of the backend {@code CollectionResponse} record
 * ({@code com.eazicut.api.collections.dto.CollectionResponse}).
 *
 * <p>Field names match the JSON on the wire verbatim — do not rename
 * without updating the backend at the same time.
 */
export type ApiCollectionResponse = {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  createdAt: string; // ISO-8601 Instant
  updatedAt: string; // ISO-8601 Instant
};
