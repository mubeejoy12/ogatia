export type ProcessStep = {
  index: string;
  title: string;
  body: string;
};

export const tailoringProcess: ProcessStep[] = [
  {
    index: "01",
    title: "Consultation",
    body: "We begin with a private consultation — in our Lagos atelier or by video for diaspora clients. Lifestyle, occasion, silhouette.",
  },
  {
    index: "02",
    title: "Measurement",
    body: "Twenty-four points of measurement, taken by hand. Digital records preserved for every future commission.",
  },
  {
    index: "03",
    title: "Fabric & Design",
    body: "Italian wools, Egyptian cottons, French linens. We curate the cloth, lining, buttons and finishing to your brief.",
  },
  {
    index: "04",
    title: "Construction",
    body: "Master tailors hand-cut and assemble each garment over four to six weeks. Fittings refine the silhouette.",
  },
  {
    index: "05",
    title: "Delivery",
    body: "Garments are pressed, finished and delivered in signature ivory packaging — or shipped insured worldwide.",
  },
];
