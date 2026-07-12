import type { Metadata } from "next";
import { Suspense } from "react";
import { PageHeader } from "@/components/sections/PageHeader";
import { CTASection } from "@/components/sections/CTASection";
import { ShopBrowser } from "@/features/shop/ShopBrowser";
import { products } from "@/lib/data/products";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "Shop",
  description:
    "Ready-to-wear and made-to-measure pieces from the Eazi Cut atelier — suits, shirts, trousers, outerwear, native wear and accessories. Filter by category, collection or price.",
  alternates: { canonical: `${site.url}/shop` },
  openGraph: {
    title: `Shop · ${site.name}`,
    description:
      "The full ready-to-wear and made-to-measure catalogue from the Eazi Cut atelier.",
    url: `${site.url}/shop`,
    type: "website",
  },
};

/**
 * JSON-LD ItemList — helps Google index the Shop as a product hub and
 * surface individual products for rich search results.
 */
const itemListSchema = {
  "@context": "https://schema.org",
  "@type": "ItemList",
  name: "Eazi Cut Shop",
  numberOfItems: products.length,
  itemListElement: products.map((p, i) => ({
    "@type": "ListItem",
    position: i + 1,
    url: `${site.url}/shop/${p.slug}`,
    name: p.name,
  })),
};

export default function ShopPage() {
  return (
    <>
      <script
        type="application/ld+json"
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{ __html: JSON.stringify(itemListSchema) }}
      />

      <PageHeader
        eyebrow="Shop"
        title="The wardrobe, ready to wear."
        intro="Every piece is cut in the Lagos atelier, from Italian and English wools, Egyptian cottons and hand-loomed Aso Oke. Ready-to-wear ships worldwide; each design can also be commissioned to your measurements."
      />

      <section
        aria-label="Product catalogue"
        className="pb-24 md:pb-32 bg-ivory"
      >
        <div className="container">
          <Suspense fallback={<ShopBrowserFallback count={products.length} />}>
            <ShopBrowser products={products} />
          </Suspense>
        </div>
      </section>

      <CTASection
        eyebrow="Bespoke"
        title="Every piece can be made to measure."
        body="Ready-to-wear is only half of what we do. Bring a piece from the shop — or a reference of your own — and we will cut it to your measurements in four to six weeks."
        primaryLabel="Book a Fitting"
        primaryHref="/contact"
        secondaryLabel="Explore Collections"
        secondaryHref="/collections"
      />
    </>
  );
}

/**
 * Server-rendered skeleton served while the client bundle hydrates.
 * No layout shift — matches the toolbar height and grid rhythm.
 */
function ShopBrowserFallback({ count }: { count: number }) {
  return (
    <div aria-hidden>
      <p className="eyebrow text-stone-500">
        <span className="rule inline-block align-middle mr-3" />
        {count} pieces
      </p>
      <div className="mt-10 border-y border-ink/10 py-6 h-24" />
    </div>
  );
}
