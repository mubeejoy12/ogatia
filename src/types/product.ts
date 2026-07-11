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
  /** Optional merchandising badge. */
  badge?: ProductBadge;
  /** Stock status. False renders the card with an "Enquire" affordance. */
  available: boolean;
};
