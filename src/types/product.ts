import type { ImageAsset, CollectionSlug } from "@/lib/assets";

/**
 * Product categories — the axis by which the Shop filters most naturally.
 * Aligned with the physical wardrobe rather than merchandiser jargon.
 */
export const productCategories = [
  "Suits",
  "Shirts",
  "Trousers",
  "Outerwear",
  "Native",
  "Accessories",
] as const;

export type ProductCategory = (typeof productCategories)[number];

/**
 * Badge — surfaced on the ProductCard in Stage 2. Data-driven so
 * merchandisers can flip these without touching components.
 */
export type ProductBadge = "New" | "Bespoke" | "Limited";

/**
 * The canonical Product record.
 *
 * The shape a future `fetch('/api/products/{slug}')` returns will be identical,
 * so components and data consumers will need no changes when the backend lands.
 */
export type Product = {
  slug: string;
  name: string;
  collection: CollectionSlug;
  category: ProductCategory;
  /** Whole Naira. Formatted through `formatNaira` for display. */
  price: number;
  /** ISO 8601 — powers the "Newest" sort. */
  createdAt: string;
  image: ImageAsset;
  /** Editorial one-liner used on the card. */
  description: string;
  /** Available sizes as they should be shown to the customer. */
  sizes: string[];
  /** One-line note on how the piece is constructed. */
  construction: string;
  /** One-line care instruction. */
  care: string;
  /** Optional merchandising badge. */
  badge?: ProductBadge;
  /** Stock status. False renders the card with an "Enquire" affordance. */
  available: boolean;
  // -------------------------------------------------------------------------
  // Optional detail-view fields — populated by the API adapter from the
  // backend ProductResponse. Absent on mock-data products; present on
  // API-sourced products where the backend supplies them.
  // -------------------------------------------------------------------------
  /** Server-issued UUID. Optional so mock data doesn't need to carry one. */
  id?: string;
  /** Longer editorial copy for the PDP; falls back to `description` if absent. */
  fullDescription?: string;
  /** Short editorial copy for cards; falls back to `description` if absent. */
  shortDescription?: string;
  /** Fabric one-liner (e.g. "Loro Piana super-120s wool"). */
  fabricType?: string | null;
  /** Colour one-liner. */
  color?: string | null;
};
