import { assets, type ImageAsset } from "@/lib/assets";

export type LookbookImage = {
  image: ImageAsset;
  caption: string;
  collection: string;
  span: "tall" | "wide" | "square";
};

export const lookbook: LookbookImage[] = [
  {
    image: assets.lookbook[0],
    caption: "The Heritage Agbada — hand-embroidered, ivory wool.",
    collection: "Lagos Heritage",
    span: "tall",
  },
  {
    image: assets.lookbook[1],
    caption: "The Noir Tuxedo — peak lapel, satin facing.",
    collection: "Ivory Wedding",
    span: "wide",
  },
  {
    image: assets.lookbook[2],
    caption: "Hand-stitched Milanese buttonhole.",
    collection: "The Onyx Bespoke",
    span: "square",
  },
  {
    image: assets.lookbook[3],
    caption: "Double-breasted charcoal, super 150s wool.",
    collection: "The Onyx Bespoke",
    span: "tall",
  },
  {
    image: assets.lookbook[4],
    caption: "The wedding party — coordinated, never identical.",
    collection: "Ivory Wedding",
    span: "wide",
  },
  {
    image: assets.lookbook[5],
    caption: "Mother-of-pearl cuff, monogrammed.",
    collection: "The Noir Tuxedo",
    span: "square",
  },
  {
    image: assets.lookbook[6],
    caption: "Inside the Lagos atelier.",
    collection: "Atelier",
    span: "tall",
  },
  {
    image: assets.lookbook[7],
    caption: "Cloth library — Italian wool, French linen.",
    collection: "Atelier",
    span: "wide",
  },
];
