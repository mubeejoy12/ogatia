/**
 * Centralised image asset registry.
 *
 * Every image referenced by the marketing site resolves through this file.
 * To swap placeholder photography for final brand assets:
 *   1. Drop the file into `/public/images/...`
 *   2. Replace the `src` value below (keep the key)
 *   3. Update `alt` to match the new photograph
 *
 * No component or data file should hardcode an image URL.
 */

export type ImageAsset = {
  src: string;
  alt: string;
  /** Rendered aspect hint; components that use `fill` can ignore. */
  width?: number;
  height?: number;
  /** Marks this asset as a temporary placeholder (dev-only affordance). */
  placeholder?: boolean;
};

// ---------------------------------------------------------------------------
// Placeholder source helper
// ---------------------------------------------------------------------------
// Every placeholder URL flows through this helper so we can flip the entire
// site to local `/public/images/...` in one edit when final assets arrive.

const unsplash = (id: string, w = 1600) =>
  `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${w}&q=85`;

const ph = (src: string, alt: string, extras: Partial<ImageAsset> = {}): ImageAsset => ({
  src,
  alt,
  placeholder: true,
  ...extras,
});

// ---------------------------------------------------------------------------
// Collection slugs — kept here so both the assets map and the data file share
// the same typed key set.
// ---------------------------------------------------------------------------

export const collectionSlugs = [
  "the-onyx-bespoke",
  "ivory-wedding",
  "lagos-heritage",
  "the-essentials",
  "the-noir-tuxedo",
  "diaspora",
] as const;

export type CollectionSlug = (typeof collectionSlugs)[number];

// ---------------------------------------------------------------------------
// The registry
// ---------------------------------------------------------------------------

export const assets = {
  brand: {
    ogImage: ph(
      unsplash("1593030103066-0093718efeb9", 1200),
      "Eazi Cut — luxury African menswear"
    ),
  },

  hero: {
    home: ph(
      unsplash("1593030103066-0093718efeb9", 2400),
      "Eazi Cut model in a black tailored suit against a warm neutral backdrop"
    ),
  },

  about: {
    craft: ph(
      unsplash("1542327897-d73f4005b533", 1800),
      "Master tailor cutting cloth on the workbench at the Eazi Cut Lagos atelier"
    ),
  },

  contact: {
    atelier: ph(
      unsplash("1520975916090-3105956dac38", 1200),
      "Interior of the Eazi Cut Lagos atelier, softly lit with bolts of cloth"
    ),
  },

  process: {
    workshop: ph(
      unsplash("1520975916090-3105956dac38", 1400),
      "A tailor pinning a jacket at the atelier"
    ),
  },

  whyChoose: {
    craft: ph(
      unsplash("1542327897-d73f4005b533", 1200),
      "Detail of hand-stitched lapel"
    ),
  },

  cta: {
    default: ph(
      unsplash("1490578474895-699cd4e2cf59", 2000),
      "A finished bespoke jacket photographed against a warm wall"
    ),
  },

  collections: {
    "the-onyx-bespoke": ph(
      unsplash("1593030103066-0093718efeb9"),
      "The Onyx Bespoke — ink-black wool two-piece suit"
    ),
    "ivory-wedding": ph(
      unsplash("1521572163474-6864f9cf17ab"),
      "Ivory Wedding — ceremonial three-piece in ivory silk wool"
    ),
    "lagos-heritage": ph(
      unsplash("1617137968427-85924c800a22"),
      "Lagos Heritage — modern kaftan and agbada with hand embroidery"
    ),
    "the-essentials": ph(
      unsplash("1507680434567-5739c80be1ac"),
      "The Essentials — ready-to-wear shirting and trousers"
    ),
    "the-noir-tuxedo": ph(
      unsplash("1594938298603-c8148c4dae35"),
      "The Noir Tuxedo — evening tailoring with peak lapels and silk facings"
    ),
    diaspora: ph(
      unsplash("1490578474895-699cd4e2cf59"),
      "Diaspora — lightweight travel tailoring"
    ),
  } satisfies Record<CollectionSlug, ImageAsset>,

  lookbook: [
    ph(unsplash("1617137968427-85924c800a22", 1400), "Embroidered ivory agbada photographed against linen"),
    ph(unsplash("1521572163474-6864f9cf17ab", 1600), "Groom in three-piece tuxedo at golden hour"),
    ph(unsplash("1507680434567-5739c80be1ac", 1200), "Detail of hand-stitched lapel buttonhole"),
    ph(unsplash("1490578474895-699cd4e2cf59", 1400), "Editorial portrait in charcoal double-breasted suit"),
    ph(unsplash("1593030103066-0093718efeb9", 1600), "Three gentlemen in coordinated wedding tailoring"),
    ph(unsplash("1594938298603-c8148c4dae35", 1200), "Gentleman adjusting cufflink in low light"),
    ph(unsplash("1542327897-d73f4005b533", 1400), "Master tailor working at a cutting table"),
    ph(unsplash("1520975916090-3105956dac38", 1600), "Editorial flat-lay of fabric swatches and shears"),
  ] as ImageAsset[],
} as const;

// ---------------------------------------------------------------------------
// Convenience accessors
// ---------------------------------------------------------------------------

export const collectionImage = (slug: CollectionSlug): ImageAsset =>
  assets.collections[slug];

export const lookbookImage = (index: number): ImageAsset => {
  const img = assets.lookbook[index];
  if (!img) throw new Error(`No lookbook image at index ${index}`);
  return img;
};
