import { assets, type CollectionSlug, type ImageAsset } from "@/lib/assets";

export type Collection = {
  slug: CollectionSlug;
  name: string;
  category: "Bespoke" | "Ready-to-Wear" | "Wedding" | "Heritage";
  description: string;
  image: ImageAsset;
  pieces: number;
};

export const collections: Collection[] = [
  {
    slug: "the-onyx-bespoke",
    name: "The Onyx Bespoke",
    category: "Bespoke",
    description:
      "Hand-finished two-piece and three-piece suiting in deep ink wool, cut to the silhouette of the modern executive.",
    image: assets.collections["the-onyx-bespoke"],
    pieces: 12,
  },
  {
    slug: "ivory-wedding",
    name: "Ivory Wedding",
    category: "Wedding",
    description:
      "Ceremonial agbadas, tuxedos and waistcoats in ivory wool, silk and cashmere — built for the day that defines a lifetime.",
    image: assets.collections["ivory-wedding"],
    pieces: 9,
  },
  {
    slug: "lagos-heritage",
    name: "Lagos Heritage",
    category: "Heritage",
    description:
      "A modern study of the traditional kaftan and agbada — heavyweight cottons, hand embroidery, refined proportion.",
    image: assets.collections["lagos-heritage"],
    pieces: 14,
  },
  {
    slug: "the-essentials",
    name: "The Essentials",
    category: "Ready-to-Wear",
    description:
      "Ready-to-wear shirting, trousers and outerwear in Italian poplins and Egyptian cotton — the everyday wardrobe of the gentleman.",
    image: assets.collections["the-essentials"],
    pieces: 22,
  },
  {
    slug: "the-noir-tuxedo",
    name: "The Noir Tuxedo",
    category: "Bespoke",
    description:
      "Evening tailoring — peak lapels, silk facings, and a silhouette built for galas, weddings and black-tie occasions.",
    image: assets.collections["the-noir-tuxedo"],
    pieces: 7,
  },
  {
    slug: "diaspora",
    name: "Diaspora",
    category: "Ready-to-Wear",
    description:
      "Lightweight tailoring for the gentleman who travels — crease-resistant wools, packable construction, shipped worldwide.",
    image: assets.collections["diaspora"],
    pieces: 11,
  },
];
