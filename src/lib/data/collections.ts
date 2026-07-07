export type Collection = {
  slug: string;
  name: string;
  category: "Bespoke" | "Ready-to-Wear" | "Wedding" | "Heritage";
  description: string;
  image: string;
  pieces: number;
};

export const collections: Collection[] = [
  {
    slug: "the-onyx-bespoke",
    name: "The Onyx Bespoke",
    category: "Bespoke",
    description:
      "Hand-finished two-piece and three-piece suiting in deep ink wool, cut to the silhouette of the modern executive.",
    image:
      "https://images.unsplash.com/photo-1593030103066-0093718efeb9?auto=format&fit=crop&w=1600&q=80",
    pieces: 12,
  },
  {
    slug: "ivory-wedding",
    name: "Ivory Wedding",
    category: "Wedding",
    description:
      "Ceremonial agbadas, tuxedos and waistcoats in ivory wool, silk and cashmere — built for the day that defines a lifetime.",
    image:
      "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1600&q=80",
    pieces: 9,
  },
  {
    slug: "lagos-heritage",
    name: "Lagos Heritage",
    category: "Heritage",
    description:
      "A modern study of the traditional kaftan and agbada — heavyweight cottons, hand embroidery, refined proportion.",
    image:
      "https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=1600&q=80",
    pieces: 14,
  },
  {
    slug: "the-essentials",
    name: "The Essentials",
    category: "Ready-to-Wear",
    description:
      "Ready-to-wear shirting, trousers and outerwear in Italian poplins and Egyptian cotton — the everyday wardrobe of the gentleman.",
    image:
      "https://images.unsplash.com/photo-1507680434567-5739c80be1ac?auto=format&fit=crop&w=1600&q=80",
    pieces: 22,
  },
  {
    slug: "the-noir-tuxedo",
    name: "The Noir Tuxedo",
    category: "Bespoke",
    description:
      "Evening tailoring — peak lapels, silk facings, and a silhouette built for galas, weddings and black-tie occasions.",
    image:
      "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?auto=format&fit=crop&w=1600&q=80",
    pieces: 7,
  },
  {
    slug: "diaspora",
    name: "Diaspora",
    category: "Ready-to-Wear",
    description:
      "Lightweight tailoring for the gentleman who travels — crease-resistant wools, packable construction, shipped worldwide.",
    image:
      "https://images.unsplash.com/photo-1490578474895-699cd4e2cf59?auto=format&fit=crop&w=1600&q=80",
    pieces: 11,
  },
];
