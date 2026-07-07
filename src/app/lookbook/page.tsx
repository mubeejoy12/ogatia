import type { Metadata } from "next";
import { PageHeader } from "@/components/sections/PageHeader";
import { LookbookGallery } from "@/components/sections/LookbookGallery";
import { CTASection } from "@/components/sections/CTASection";

export const metadata: Metadata = {
  title: "Lookbook",
  description:
    "The Eazi Cut lookbook — editorial photography of bespoke tailoring, wedding suiting and heritage menswear.",
};

export default function LookbookPage() {
  return (
    <>
      <PageHeader
        eyebrow="Lookbook"
        title="The house, in photographs."
        intro="Editorial studies of recent commissions — from the heritage agbada to the noir tuxedo. Photographed in and around the Lagos atelier."
      />
      <LookbookGallery />
      <CTASection
        eyebrow="Be the next portrait"
        title="Commission a wardrobe worth photographing."
        body="From a first suit to a wedding party of twelve, every commission begins with a quiet conversation."
        primaryLabel="Book a Fitting"
        secondaryLabel="Browse Collections"
        secondaryHref="/collections"
      />
    </>
  );
}
