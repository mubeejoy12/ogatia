export type LookbookImage = {
  src: string;
  alt: string;
  caption: string;
  collection: string;
  span: "tall" | "wide" | "square";
};

export const lookbook: LookbookImage[] = [
  {
    src: "https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=1400&q=80",
    alt: "Embroidered ivory agbada photographed against linen",
    caption: "The Heritage Agbada — hand-embroidered, ivory wool.",
    collection: "Lagos Heritage",
    span: "tall",
  },
  {
    src: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1600&q=80",
    alt: "Groom in three-piece tuxedo at golden hour",
    caption: "The Noir Tuxedo — peak lapel, satin facing.",
    collection: "Ivory Wedding",
    span: "wide",
  },
  {
    src: "https://images.unsplash.com/photo-1507680434567-5739c80be1ac?auto=format&fit=crop&w=1200&q=80",
    alt: "Detail of hand-stitched lapel buttonhole",
    caption: "Hand-stitched Milanese buttonhole.",
    collection: "The Onyx Bespoke",
    span: "square",
  },
  {
    src: "https://images.unsplash.com/photo-1490578474895-699cd4e2cf59?auto=format&fit=crop&w=1400&q=80",
    alt: "Editorial portrait in charcoal double-breasted suit",
    caption: "Double-breasted charcoal, super 150s wool.",
    collection: "The Onyx Bespoke",
    span: "tall",
  },
  {
    src: "https://images.unsplash.com/photo-1593030103066-0093718efeb9?auto=format&fit=crop&w=1600&q=80",
    alt: "Three gentlemen in coordinated wedding tailoring",
    caption: "The wedding party — coordinated, never identical.",
    collection: "Ivory Wedding",
    span: "wide",
  },
  {
    src: "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?auto=format&fit=crop&w=1200&q=80",
    alt: "Gentleman adjusting cufflink in low light",
    caption: "Mother-of-pearl cuff, monogrammed.",
    collection: "The Noir Tuxedo",
    span: "square",
  },
  {
    src: "https://images.unsplash.com/photo-1542327897-d73f4005b533?auto=format&fit=crop&w=1400&q=80",
    alt: "Master tailor working at a cutting table",
    caption: "Inside the Lagos atelier.",
    collection: "Atelier",
    span: "tall",
  },
  {
    src: "https://images.unsplash.com/photo-1520975916090-3105956dac38?auto=format&fit=crop&w=1600&q=80",
    alt: "Editorial flat-lay of fabric swatches and shears",
    caption: "Cloth library — Italian wool, French linen.",
    collection: "Atelier",
    span: "wide",
  },
];
