import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { CTASection } from "@/components/sections/CTASection";
import { Stagger, StaggerItem } from "@/components/motion/Reveal";
import { products } from "@/lib/data/products";
import { formatNaira } from "@/lib/format";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "Shop",
  description:
    "Ready-to-wear and made-to-measure pieces from the Eazi Cut atelier — suits, shirts, trousers, outerwear, native wear and accessories.",
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
          <p className="eyebrow text-stone-500">
            <span className="rule inline-block align-middle mr-3" />
            {products.length} pieces in the current collection
          </p>

          <Stagger className="mt-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-16">
            {products.map((product, i) => (
              <StaggerItem key={product.slug}>
                <Link
                  href={`/shop/${product.slug}`}
                  aria-label={`View ${product.name}`}
                  className="group block focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory"
                >
                  <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
                    <Image
                      src={product.image.src}
                      alt={`${product.name} — ${product.image.alt}`}
                      fill
                      sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
                      priority={i < 3}
                      className="object-cover transition-transform duration-1200 ease-editorial group-hover:scale-105"
                    />
                  </div>
                  <div className="mt-5">
                    <p className="eyebrow text-stone-500">{product.category}</p>
                    <h3 className="mt-2 font-display text-xl md:text-2xl tracking-tightest">
                      {product.name}
                    </h3>
                    <p className="mt-3 text-ink font-sans text-sm">
                      {formatNaira(product.price)}
                    </p>
                  </div>
                </Link>
              </StaggerItem>
            ))}
          </Stagger>
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
