import { assets } from "@/lib/assets";
import { notFound } from "next/navigation";
import type { Product, ProductCategory } from "@/types/product";

/**
 * Default size ranges per category. Products may override with their own
 * `sizes` array if the piece runs to a different scale (e.g. one-size
 * accessories or a bespoke run that only ships in three sizes).
 */
const DEFAULT_SIZES: Record<ProductCategory, string[]> = {
  Suits: ["46", "48", "50", "52", "54", "56"],
  Shirts: ["S", "M", "L", "XL"],
  Trousers: ["30", "32", "34", "36", "38"],
  Outerwear: ["S", "M", "L", "XL"],
  Native: ["S", "M", "L", "XL", "XXL"],
  Accessories: ["One Size"],
};

/**
 * The product catalogue.
 *
 * Curated set of eighteen pieces, three per collection, that reflect the
 * shape of the house's real wardrobe. This is the single source of truth
 * until a CMS or backend takes over — the schema is deliberately identical
 * to what a future `GET /api/products` will return, so no component will
 * change when that swap happens.
 *
 * Images currently reuse the collection hero. When per-product photography
 * arrives we introduce `assets.products` and swap the `image` reference;
 * consumers do not need to change.
 */
export const products: Product[] = [
  // The Onyx Bespoke — 3 pieces ------------------------------------------
  {
    slug: "onyx-two-piece-suit",
    name: "The Onyx Two-Piece",
    collection: "the-onyx-bespoke",
    category: "Suits",
    price: 650000,
    createdAt: "2026-05-04",
    image: assets.collections["the-onyx-bespoke"],
    description:
      "Soft-shouldered two-piece in Loro Piana super-120s ink wool. Cut for the boardroom.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Full-canvas construction, hand-padded lapels, hand-attached collar.",
    care: "Dry-clean only. Rest on a broad-shouldered hanger between wears.",
    badge: "Bespoke",
    available: true,
  },
  {
    slug: "onyx-three-piece-suit",
    name: "The Onyx Three-Piece",
    collection: "the-onyx-bespoke",
    category: "Suits",
    price: 780000,
    createdAt: "2026-05-04",
    image: assets.collections["the-onyx-bespoke"],
    description:
      "The two-piece completed by a single-breasted waistcoat — a wardrobe for weeks abroad.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Full-canvas jacket and waistcoat, hand-padded lapels, silk-cotton lining.",
    care: "Dry-clean only. Store trousers on a clamp hanger to hold the crease.",
    badge: "Bespoke",
    available: true,
  },
  {
    slug: "ink-overcoat",
    name: "The Ink Overcoat",
    collection: "the-onyx-bespoke",
    category: "Outerwear",
    price: 520000,
    createdAt: "2026-04-18",
    image: assets.collections["the-onyx-bespoke"],
    description:
      "Full-length cashmere-blend overcoat with peak lapels, cut to sit clean over tailoring.",
    sizes: DEFAULT_SIZES.Outerwear,
    construction:
      "Half-canvas, taped seams, functional horn button placket.",
    care: "Dry-clean seasonally. Brush after each wear to lift dust from the nap.",
    available: true,
  },

  // Ivory Wedding — 3 pieces --------------------------------------------
  {
    slug: "ivory-three-piece",
    name: "The Ivory Three-Piece",
    collection: "ivory-wedding",
    category: "Suits",
    price: 950000,
    createdAt: "2026-06-11",
    image: assets.collections["ivory-wedding"],
    description:
      "Escorial wool three-piece in soft ivory — the anchor of the ceremonial wardrobe.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Full-canvas, hand-padded lapels, cashmere-silk lining, functional cuff buttons.",
    care: "Dry-clean only. Store in a breathable cotton garment bag between events.",
    badge: "New",
    available: true,
  },
  {
    slug: "cream-agbada",
    name: "The Cream Agbada",
    collection: "ivory-wedding",
    category: "Native",
    price: 850000,
    createdAt: "2026-06-11",
    image: assets.collections["ivory-wedding"],
    description:
      "Ceremonial agbada in cream silk-blend cotton, finished with hand-embroidered neckline.",
    sizes: DEFAULT_SIZES.Native,
    construction:
      "Hand-embroidered neckline, French seams, cotton-silk under-cap and trouser.",
    care: "Hand-wash cold or dry-clean. Iron the placket only, not the embroidery.",
    badge: "Bespoke",
    available: true,
  },
  {
    slug: "champagne-tuxedo",
    name: "The Champagne Tuxedo",
    collection: "ivory-wedding",
    category: "Suits",
    price: 890000,
    createdAt: "2026-05-22",
    image: assets.collections["ivory-wedding"],
    description:
      "Shawl-lapelled tuxedo in champagne wool, silk-faced, for the evening reception.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Full-canvas, silk-faced shawl lapels, satin-piped trouser side seam.",
    care: "Dry-clean only. Spot-test any stain remover on the lining first.",
    badge: "Limited",
    available: false,
  },

  // Lagos Heritage — 3 pieces -------------------------------------------
  {
    slug: "full-agbada",
    name: "The Full Agbada",
    collection: "lagos-heritage",
    category: "Native",
    price: 620000,
    createdAt: "2026-04-02",
    image: assets.collections["lagos-heritage"],
    description:
      "Three-piece agbada in heavyweight Aso Oke — trousers, kaftan and outer robe.",
    sizes: DEFAULT_SIZES.Native,
    construction:
      "Hand-loomed Aso Oke, French seams, hand-embroidered chest panel.",
    care: "Dry-clean only. Steam gently — do not iron over the woven relief.",
    available: true,
  },
  {
    slug: "damask-kaftan",
    name: "The Damask Kaftan",
    collection: "lagos-heritage",
    category: "Native",
    price: 320000,
    createdAt: "2026-05-30",
    image: assets.collections["lagos-heritage"],
    description:
      "Hand-loomed damask kaftan with mandarin collar and Etuk embroidery at the placket.",
    sizes: DEFAULT_SIZES.Native,
    construction:
      "Mandarin collar, mother-of-pearl buttons, hand-embroidered placket detail.",
    care: "Hand-wash cold, dry flat. Iron on the reverse of the placket only.",
    badge: "New",
    available: true,
  },
  {
    slug: "aso-oke-two-piece",
    name: "The Aso Oke Two-Piece",
    collection: "lagos-heritage",
    category: "Native",
    price: 480000,
    createdAt: "2026-03-12",
    image: assets.collections["lagos-heritage"],
    description:
      "Kaftan and trouser in matching Aso Oke — a considered alternative to the full agbada.",
    sizes: DEFAULT_SIZES.Native,
    construction:
      "Matched Aso Oke throughout, French seams, cotton under-lining.",
    care: "Dry-clean only. Steam — do not iron — the woven surface.",
    available: true,
  },

  // The Essentials — 3 pieces -------------------------------------------
  {
    slug: "poplin-shirt",
    name: "The Poplin Shirt",
    collection: "the-essentials",
    category: "Shirts",
    price: 95000,
    createdAt: "2026-06-20",
    image: assets.collections["the-essentials"],
    description:
      "Italian cotton poplin shirt with spread collar and mother-of-pearl buttons.",
    sizes: DEFAULT_SIZES.Shirts,
    construction:
      "Single-needle side seams, split yoke, removable collar stays.",
    care: "Machine-wash cold, hang to dry. Iron collar and cuffs damp.",
    badge: "New",
    available: true,
  },
  {
    slug: "charcoal-trouser",
    name: "The Charcoal Trouser",
    collection: "the-essentials",
    category: "Trousers",
    price: 145000,
    createdAt: "2026-05-15",
    image: assets.collections["the-essentials"],
    description:
      "High-rise wool trouser with double reverse pleat — the workhorse of the wardrobe.",
    sizes: DEFAULT_SIZES.Trousers,
    construction:
      "Curtained waistband, hand-set French fly, cotton pocketing.",
    care: "Dry-clean between seasons. Steam creases to preserve the pleat.",
    available: true,
  },
  {
    slug: "ink-blazer",
    name: "The Ink Blazer",
    collection: "the-essentials",
    category: "Suits",
    price: 285000,
    createdAt: "2026-06-01",
    image: assets.collections["the-essentials"],
    description:
      "Unstructured single-breasted blazer in Italian wool — off-duty tailoring.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Unlined, half-canvas shoulder, functional four-button cuff.",
    care: "Dry-clean only. Brush the collar between wears.",
    available: true,
  },

  // The Noir Tuxedo — 3 pieces ------------------------------------------
  {
    slug: "peak-lapel-tuxedo",
    name: "The Peak-Lapel Tuxedo",
    collection: "the-noir-tuxedo",
    category: "Suits",
    price: 820000,
    createdAt: "2026-04-25",
    image: assets.collections["the-noir-tuxedo"],
    description:
      "Superfine 130s wool tuxedo with grosgrain peak lapels and satin-galloon trousers.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Full-canvas, grosgrain-faced lapels, satin galloon down the outer leg.",
    care: "Dry-clean only. Hang on a broad wooden hanger; never fold.",
    badge: "Bespoke",
    available: true,
  },
  {
    slug: "shawl-collar-dinner-jacket",
    name: "The Shawl-Collar Dinner Jacket",
    collection: "the-noir-tuxedo",
    category: "Suits",
    price: 690000,
    createdAt: "2026-03-30",
    image: assets.collections["the-noir-tuxedo"],
    description:
      "Single-button dinner jacket in ink wool with silk shawl collar — evening lightness.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Full-canvas, silk shawl collar, jetted pockets with satin trim.",
    care: "Dry-clean only. Store on a shaped hanger away from direct light.",
    available: true,
  },
  {
    slug: "evening-trouser",
    name: "The Evening Trouser",
    collection: "the-noir-tuxedo",
    category: "Trousers",
    price: 180000,
    createdAt: "2026-04-10",
    image: assets.collections["the-noir-tuxedo"],
    description:
      "Formal trouser in Superfine 130s wool with a satin galloon down the outer leg.",
    sizes: DEFAULT_SIZES.Trousers,
    construction:
      "Curtained waistband, satin galloon, silk lining to the knee.",
    care: "Dry-clean only. Rest on a clamp hanger to hold the crease.",
    available: true,
  },

  // Diaspora — 3 pieces --------------------------------------------------
  {
    slug: "travel-suit",
    name: "The Travel Suit",
    collection: "diaspora",
    category: "Suits",
    price: 420000,
    createdAt: "2026-05-08",
    image: assets.collections["diaspora"],
    description:
      "Crease-resistant Vitale Barberis Canonico high-twist wool — engineered for the suitcase.",
    sizes: DEFAULT_SIZES.Suits,
    construction:
      "Half-canvas, high-twist wool, packable jacket and trouser.",
    care: "Dry-clean seasonally. Steam creases out of the case; the wool self-recovers.",
    badge: "New",
    available: true,
  },
  {
    slug: "weekender-jacket",
    name: "The Weekender Jacket",
    collection: "diaspora",
    category: "Outerwear",
    price: 310000,
    createdAt: "2026-06-02",
    image: assets.collections["diaspora"],
    description:
      "Unstructured cotton-linen jacket — the answer to a warm-weather boardroom.",
    sizes: DEFAULT_SIZES.Outerwear,
    construction:
      "Unlined, patch pockets, natural horn buttons.",
    care: "Dry-clean or gentle hand-wash cold. Reshape damp; hang to dry.",
    available: true,
  },
  {
    slug: "packable-trouser",
    name: "The Packable Trouser",
    collection: "diaspora",
    category: "Trousers",
    price: 155000,
    createdAt: "2026-04-14",
    image: assets.collections["diaspora"],
    description:
      "Half-canvas high-twist wool trouser — arrives from the case unrumpled.",
    sizes: DEFAULT_SIZES.Trousers,
    construction:
      "Half-canvas, high-twist wool, extended tab closure, split waistband.",
    care: "Dry-clean seasonally. Steam gently to reset the crease.",
    available: true,
  },
];

// ---------------------------------------------------------------------------
// Accessors
// ---------------------------------------------------------------------------

/** Return a product by slug or trigger a Next.js 404. */
export function getProduct(slug: string): Product {
  const found = products.find((p) => p.slug === slug);
  if (!found) notFound();
  return found;
}

/** Every slug — used by `generateStaticParams` in Ticket 002.3. */
export function getProductSlugs(): string[] {
  return products.map((p) => p.slug);
}

/**
 * Products related to the given one — same collection first, filling with
 * the same category if the collection is thin. Deterministic; excludes the
 * subject product itself.
 */
export function getRelatedProducts(slug: string, limit = 3): Product[] {
  const target = products.find((p) => p.slug === slug);
  if (!target) return [];

  const sameCollection = products.filter(
    (p) => p.collection === target.collection && p.slug !== target.slug
  );
  const sameCategory = products.filter(
    (p) =>
      p.category === target.category &&
      p.collection !== target.collection &&
      p.slug !== target.slug
  );

  return [...sameCollection, ...sameCategory].slice(0, limit);
}

/**
 * Delivery-time hint keyed by category. Bespoke suits go longer; ready-to-
 * wear pieces ship in days.
 */
export function getDeliveryEstimate(product: Product): string {
  if (product.badge === "Bespoke") return "4 – 6 weeks (bespoke, cut to measure)";
  if (!product.available) return "By enquiry — write to the atelier";
  return "3 – 5 business days (ready-to-wear, ships from Lagos)";
}
