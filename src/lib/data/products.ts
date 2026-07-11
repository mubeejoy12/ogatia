import { assets } from "@/lib/assets";
import { notFound } from "next/navigation";
import type { Product } from "@/types/product";

/**
 * The product catalogue.
 *
 * Curated set of eighteen pieces, three per collection, that reflect the
 * shape of the house's real wardrobe. This is the single source of truth
 * until a CMS or backend takes over — the schema is deliberately identical
 * to what a future `GET /api/products` will return, so no component will
 * change when that swap happens.
 *
 * Images currently reuse the collection hero — Stage 1 scope. When
 * per-product photography arrives we introduce `assets.products` and
 * swap the `image` reference; consumers do not need to change.
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
