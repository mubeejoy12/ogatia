import type { ImageAsset, CollectionSlug } from "@/lib/assets";
import { collectionSlugs } from "@/lib/assets";
import type { PagedResponse } from "@/types/api/envelope";
import type {
  ApiProductImage,
  ApiProductResponse,
} from "@/types/api/product";
import type {
  Product,
  ProductBadge,
  ProductCategory,
} from "@/types/product";
import { productCategories } from "@/types/product";

/**
 * Adapter — normalise a backend {@link ApiProductResponse} into the
 * frontend view model {@link Product} that existing components
 * (ProductCard, PDP, etc.) already consume.
 *
 * <p>Every field is derived deterministically from the backend response;
 * where the backend lacks a counterpart (currently `construction` and
 * `care`), the adapter emits an empty string and the UI hides it. The
 * shortlist of contract mismatches is documented on F003 Stage 1.
 */
export function mapApiProduct(api: ApiProductResponse): Product {
  return {
    slug: api.slug,
    name: api.name,
    category: resolveCategory(api),
    collection: resolveCollectionSlug(api),
    price: api.price,
    createdAt: api.createdAt,
    image: resolveImage(api),
    description: api.shortDescription,
    sizes: api.availableSizes,
    // Backend does not yet carry construction / care fields; the PDP
    // treats an empty string as "not surfaced". When B003+ adds them
    // we widen the ApiProductResponse type and populate here.
    construction: "",
    care: "",
    badge: resolveBadge(api),
    available: isAvailable(api),
    // Optional detail-view fields — API-sourced products carry them,
    // mock-data products don't. The PDP hides empty rows and falls back
    // to `description` when full/short descriptions are absent.
    id: api.id,
    fullDescription: api.fullDescription,
    shortDescription: api.shortDescription,
    fabricType: api.fabricType,
    color: api.color,
  };
}

/**
 * Convenience — map every item of a {@link PagedResponse<ApiProductResponse>}
 * into the view-model list a component can render directly.
 */
export function mapApiProductPage(
  page: PagedResponse<ApiProductResponse>
): {
  items: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
} {
  return {
    items: page.content.map(mapApiProduct),
    page: page.page,
    size: page.size,
    totalElements: page.totalElements,
    totalPages: page.totalPages,
    first: page.first,
    last: page.last,
    empty: page.empty,
  };
}

// ---------------------------------------------------------------------------
// Field-level derivations
// ---------------------------------------------------------------------------

const CATEGORY_SET = new Set<string>(productCategories);
const COLLECTION_SET = new Set<string>(collectionSlugs);

/**
 * Match the backend category *name* against the frontend's
 * {@link ProductCategory} union. When the storefront is seeded with
 * matching taxonomy data the two agree; when they don't (or when the
 * product is uncategorised), we fall back to a defensive default so
 * the UI never breaks.
 */
function resolveCategory(api: ApiProductResponse): ProductCategory {
  const name = api.category?.name;
  if (name && CATEGORY_SET.has(name)) {
    return name as ProductCategory;
  }
  return "Accessories";
}

/**
 * Match the backend collection *slug* against the frontend's
 * {@link CollectionSlug} union. Falls back to the first known slug
 * if the backend collection isn't part of the union — again defensive.
 */
function resolveCollectionSlug(api: ApiProductResponse): CollectionSlug {
  const slug = api.collection?.slug;
  if (slug && COLLECTION_SET.has(slug)) {
    return slug as CollectionSlug;
  }
  return collectionSlugs[0];
}

/**
 * Pick the primary image. In order:
 *   1. an image with `primary: true`
 *   2. the first image by `sortOrder`
 *   3. a neutral placeholder (empty src, empty alt) so the UI has
 *      something to render without crashing
 */
function resolveImage(api: ApiProductResponse): ImageAsset {
  const primary = api.images.find((img) => img.primary);
  const chosen = primary ?? sortedFirst(api.images);
  if (!chosen) {
    return { src: "", alt: api.name };
  }
  return {
    src: chosen.url,
    alt: chosen.alt ?? api.name,
  };
}

function sortedFirst(images: ApiProductImage[]): ApiProductImage | undefined {
  if (images.length === 0) return undefined;
  return [...images].sort((a, b) => a.sortOrder - b.sortOrder)[0];
}

/**
 * Merchandising badge. Backend carries three orthogonal flags
 * (`featured`, `newArrival`, `bestseller`). The frontend badge
 * union only has `"New" | "Bespoke" | "Limited"` — the current
 * pragma is to surface `newArrival` as "New" and leave the other
 * two badge tokens as dead literals until a later cleanup ticket.
 */
function resolveBadge(api: ApiProductResponse): ProductBadge | undefined {
  if (api.newArrival) return "New";
  return undefined;
}

/**
 * Computed availability — matches the backend's `available=true`
 * filter axis (see `ProductSpecifications.isAvailable`).
 */
function isAvailable(api: ApiProductResponse): boolean {
  return api.status === "ACTIVE" && api.stockQuantity > 0;
}
