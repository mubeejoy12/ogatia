import type { Metadata } from "next";
import { Suspense } from "react";
import { PageHeader } from "@/components/sections/PageHeader";
import { CTASection } from "@/components/sections/CTASection";
import { ShopBrowser, type ProductPage } from "@/features/shop/ShopBrowser";
import { fetchProducts } from "@/lib/api/products";
import { mapApiProductPage } from "@/lib/api/adapters/product";
import {
  DEFAULT_PAGE_SIZE,
  readFilterState,
  readPageIndex,
  toApiFilter,
  type RawSearchParams,
} from "@/features/shop/backendFilter";
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

// This page reads live URL params + calls the backend on every request,
// so opting it out of static rendering makes the contract explicit.
export const dynamic = "force-dynamic";

export default async function ShopPage({
  searchParams,
}: {
  searchParams: Promise<RawSearchParams>;
}) {
  const raw = await searchParams;
  const filterState = readFilterState(raw);
  const currentPage = readPageIndex(raw);

  // Backend uses 0-indexed pages; the URL and UI use 1-indexed. Translate
  // at the boundary. Client sort mode "featured" collapses to the
  // backend's default (createdAt,desc); everything else maps 1:1.
  const backendPage = await fetchProducts({
    ...toApiFilter(filterState),
    page: currentPage - 1,
    size: DEFAULT_PAGE_SIZE,
  });

  const mapped = mapApiProductPage(backendPage);
  const page: ProductPage = {
    items: mapped.items,
    currentPage,
    totalPages: Math.max(mapped.totalPages, 1),
    totalElements: mapped.totalElements,
  };

  // JSON-LD reflects what the customer currently sees, not the full
  // catalogue — Google prefers pages that describe themselves honestly.
  const itemListSchema = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: "Eazi Cut Shop",
    numberOfItems: mapped.totalElements,
    itemListElement: mapped.items.map((p, i) => ({
      "@type": "ListItem",
      position: (currentPage - 1) * DEFAULT_PAGE_SIZE + i + 1,
      url: `${site.url}/shop/${p.slug}`,
      name: p.name,
    })),
  };

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
          <Suspense
            fallback={<ShopBrowserFallback count={page.totalElements} />}
          >
            <ShopBrowser page={page} />
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
 * Server-rendered skeleton served while the client bundle hydrates the
 * toolbar. No layout shift — matches the header + toolbar height.
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
