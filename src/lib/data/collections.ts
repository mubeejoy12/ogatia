import { assets, type CollectionSlug, type ImageAsset } from "@/lib/assets";
import { notFound } from "next/navigation";

export type CollectionCategory =
  | "Bespoke"
  | "Ready-to-Wear"
  | "Wedding"
  | "Heritage";

/**
 * Collection — the schema every downstream consumer relies on.
 *
 * This is the single source of truth until a CMS or backend takes over.
 * When Ticket 003 (Shop) lands, `pieces` will be replaced by a live count
 * from the product service; the shape stays identical, so no component
 * needs to change.
 */
export type Collection = {
  slug: CollectionSlug;
  name: string;
  category: CollectionCategory;
  /** One-sentence summary used on cards. */
  description: string;
  /** Poetic one-liner used as the detail-page hero sub-title. */
  tagline: string;
  /** Multi-paragraph editorial story for the detail page. */
  story: string[];
  /** Names of the signature pieces in this collection. */
  signaturePieces: string[];
  /** Fabric note — one line. */
  fabric: string;
  /** From-price for commercial hint. Undefined until pricing is set. */
  startingPrice?: string;
  /** Card image. */
  image: ImageAsset;
  /** Current piece count. Swap for API count in Phase 2. */
  pieces: number;
};

export const collections: Collection[] = [
  {
    slug: "the-onyx-bespoke",
    name: "The Onyx Bespoke",
    category: "Bespoke",
    description:
      "Hand-finished two-piece and three-piece suiting in deep ink wool, cut to the silhouette of the modern executive.",
    tagline: "Black wool, cut clean.",
    story: [
      "The Onyx Bespoke is the wardrobe on which every Eazi Cut commission rests. A study in the discipline of dark tailoring — soft-shouldered, narrow-lapelled, cut close through the waist without ever whispering of restriction.",
      "Every suit begins with a two-hour consultation, a hand-drawn pattern, and a fabric library that stretches from Huddersfield to Biella. Each commission is delivered in four to six weeks, and the pattern is kept on file for a lifetime.",
    ],
    signaturePieces: [
      "The Onyx Two-Piece",
      "The Onyx Three-Piece",
      "The Ink Overcoat",
      "The Charcoal Trouser",
    ],
    fabric: "Loro Piana wool, Huddersfield super-120s.",
    startingPrice: "From ₦550,000",
    image: assets.collections["the-onyx-bespoke"],
    pieces: 12,
  },
  {
    slug: "ivory-wedding",
    name: "Ivory Wedding",
    category: "Wedding",
    description:
      "Ceremonial agbadas, tuxedos and waistcoats in ivory wool, silk and cashmere — built for the day that defines a lifetime.",
    tagline: "For the day that photographs forever.",
    story: [
      "Ivory Wedding is our full ceremonial wardrobe — every piece designed to sit at the centre of the frame. Silks that catch the last light of the ceremony, wools that hold a knife-edge crease from morning to reception, cashmere linings finished by hand.",
      "We tailor entire wedding parties across three continents, coordinating measurements, fittings and fabric across time zones. The last piece leaves the atelier packaged in ivory boxes, wrapped in tissue, ready to be worn once and remembered forever.",
    ],
    signaturePieces: [
      "The Ivory Three-Piece",
      "The Cream Agbada",
      "The Champagne Tuxedo",
      "The Wedding Waistcoat",
    ],
    fabric: "Escorial wool, cashmere-silk lining.",
    startingPrice: "From ₦850,000",
    image: assets.collections["ivory-wedding"],
    pieces: 9,
  },
  {
    slug: "lagos-heritage",
    name: "Lagos Heritage",
    category: "Heritage",
    description:
      "A modern study of the traditional kaftan and agbada — heavyweight cottons, hand embroidery, refined proportion.",
    tagline: "Heritage, drawn with a modern hand.",
    story: [
      "Lagos Heritage is our reading of the classical Nigerian wardrobe. We work with heavyweight Aso Oke cottons, hand-loomed damask and silk-blend brocades — proportioned for the modern silhouette, without loosening the shoulder or the story.",
      "Every piece is finished with hand embroidery in the atelier's own workshop. The result: heritage garments cut sharp enough for a boardroom, ceremonial enough for a coronation.",
    ],
    signaturePieces: [
      "The Full Agbada",
      "The Kaftan",
      "The Aso Oke Two-Piece",
      "The Embroidered Cap",
    ],
    fabric: "Aso Oke, hand-loomed damask, silk brocade.",
    startingPrice: "From ₦450,000",
    image: assets.collections["lagos-heritage"],
    pieces: 14,
  },
  {
    slug: "the-essentials",
    name: "The Essentials",
    category: "Ready-to-Wear",
    description:
      "Ready-to-wear shirting, trousers and outerwear in Italian poplins and Egyptian cotton — the everyday wardrobe of the gentleman.",
    tagline: "The wardrobe underneath the wardrobe.",
    story: [
      "The Essentials is our ready-to-wear line — the shirt that fits before the suit is worn, the trouser that carries the tie loose, the overcoat that walks between the two.",
      "Cut from Italian poplins, Egyptian cotton twills, and lightweight English worsteds. Available in a considered palette of ink, ivory, stone and navy — engineered to layer with every commission we make.",
    ],
    signaturePieces: [
      "The Poplin Shirt",
      "The Charcoal Trouser",
      "The Ivory Polo",
      "The Ink Blazer",
    ],
    fabric: "Italian poplin, Egyptian cotton, English worsted.",
    startingPrice: "From ₦85,000",
    image: assets.collections["the-essentials"],
    pieces: 22,
  },
  {
    slug: "the-noir-tuxedo",
    name: "The Noir Tuxedo",
    category: "Bespoke",
    description:
      "Evening tailoring — peak lapels, silk facings, and a silhouette built for galas, weddings and black-tie occasions.",
    tagline: "The last suit you'll ever need to buy.",
    story: [
      "The Noir Tuxedo is our house's evening statement. Peak lapels in grosgrain silk, cummerbund or single-button waistcoat, trousers finished with a satin galloon down the leg.",
      "Cut for the gala, the wedding, the fundraiser, the film premiere. Made once, worn for a decade — and altered by the same tailor who cut it if the silhouette shifts.",
    ],
    signaturePieces: [
      "The Peak-Lapel Tuxedo",
      "The Shawl-Collar Dinner Jacket",
      "The Cummerbund",
      "The Evening Trouser",
    ],
    fabric: "Superfine 130s wool, grosgrain silk facings.",
    startingPrice: "From ₦700,000",
    image: assets.collections["the-noir-tuxedo"],
    pieces: 7,
  },
  {
    slug: "diaspora",
    name: "Diaspora",
    category: "Ready-to-Wear",
    description:
      "Lightweight tailoring for the gentleman who travels — crease-resistant wools, packable construction, shipped worldwide.",
    tagline: "Tailored to travel.",
    story: [
      "Diaspora was drawn for the client who moves. Lightweight, crease-resistant wools, packable construction, and half-canvas that survives a folded suitcase. Cut to the same house silhouette as our bespoke — but ready to wear, and ready to ship.",
      "Every commission is delivered insured worldwide, in signature ivory packaging, from Lagos to London, Houston, or Dubai.",
    ],
    signaturePieces: [
      "The Travel Suit",
      "The Weekender Jacket",
      "The Packable Trouser",
      "The Diaspora Overcoat",
    ],
    fabric: "Vitale Barberis Canonico high-twist wool.",
    startingPrice: "From ₦380,000",
    image: assets.collections["diaspora"],
    pieces: 11,
  },
];

// ---------------------------------------------------------------------------
// Accessors
// ---------------------------------------------------------------------------

/**
 * Return a collection by slug or trigger a Next.js 404.
 *
 * When the backend takes over this becomes an async fetch — the shape stays
 * identical, and dynamic routes will simply `await` it.
 */
export function getCollection(slug: string): Collection {
  const found = collections.find((c) => c.slug === slug);
  if (!found) notFound();
  return found;
}

/** Every slug — used by `generateStaticParams`. */
export function getCollectionSlugs(): CollectionSlug[] {
  return collections.map((c) => c.slug);
}

/**
 * Two other collections to display at the foot of a detail page.
 * Deterministic — same neighbours every visit, no client-only randomness.
 */
export function getRelatedCollections(
  currentSlug: string,
  limit = 2
): Collection[] {
  const idx = collections.findIndex((c) => c.slug === currentSlug);
  if (idx === -1) return collections.slice(0, limit);
  const rotated = [...collections.slice(idx + 1), ...collections.slice(0, idx)];
  return rotated.slice(0, limit);
}
