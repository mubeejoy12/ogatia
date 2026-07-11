import type { Metadata } from "next";
import { PageHeader } from "@/components/sections/PageHeader";
import { CTASection } from "@/components/sections/CTASection";
import { CollectionCard } from "@/components/sections/CollectionCard";
import { Stagger, StaggerItem } from "@/components/motion/Reveal";
import { collections } from "@/lib/data/collections";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "Collections",
  description:
    "Six collections from the Eazi Cut atelier — from The Onyx Bespoke to Ivory Wedding, Lagos Heritage and Diaspora. Each a complete wardrobe.",
  alternates: { canonical: `${site.url}/collections` },
  openGraph: {
    title: `Collections · ${site.name}`,
    description:
      "Six wardrobes from the Eazi Cut atelier — bespoke, ready-to-wear, wedding and heritage.",
    url: `${site.url}/collections`,
    type: "website",
  },
};

/**
 * JSON-LD `CollectionPage` — helps Google index this route as a category
 * hub. Each item points at its detail page.
 */
const collectionPageSchema = {
  "@context": "https://schema.org",
  "@type": "CollectionPage",
  name: "Collections",
  url: `${site.url}/collections`,
  isPartOf: { "@type": "WebSite", name: site.name, url: site.url },
  hasPart: collections.map((c) => ({
    "@type": "CreativeWork",
    name: c.name,
    url: `${site.url}/collections/${c.slug}`,
    description: c.description,
    genre: c.category,
  })),
};

export default function CollectionsPage() {
  return (
    <>
      <script
        type="application/ld+json"
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(collectionPageSchema),
        }}
      />

      <PageHeader
        eyebrow="Collections"
        title="Six wardrobes, one house."
        intro="Each collection is conceived as a complete wardrobe — from the boardroom to the ballroom, from the heritage agbada to the lightweight diaspora suit. Choose one, or commission across all."
      />

      <section
        aria-label="Collection index"
        className="pb-24 md:pb-32 bg-ivory"
      >
        <div className="container">
          <Stagger className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-20">
            {collections.map((c, i) => (
              <StaggerItem key={c.slug}>
                <CollectionCard collection={c} index={i} priority={i < 3} />
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </section>

      <CTASection
        eyebrow="Bespoke"
        title="Cannot find what you are looking for?"
        body="Every Eazi Cut commission can be reimagined to your brief. Bring a reference, a fabric, a memory — we build the rest."
        primaryLabel="Start a Commission"
        primaryHref="/contact"
        secondaryLabel="See the Lookbook"
        secondaryHref="/lookbook"
      />
    </>
  );
}
