import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { CTASection } from "@/components/sections/CTASection";
import { SectionHeader } from "@/components/sections/SectionHeader";
import { Reveal, Stagger, StaggerItem } from "@/components/motion/Reveal";
import { ProductActions } from "@/features/shop/ProductActions";
import { ProductCard } from "@/components/shop/ProductCard";
import { SizeGuide } from "@/components/shop/SizeGuide";
import {
  getProduct,
  getProductSlugs,
  getRelatedProducts,
  getDeliveryEstimate,
} from "@/lib/data/products";
import { collections } from "@/lib/data/collections";
import { formatNaira } from "@/lib/format";
import { site } from "@/lib/site";

// ---------------------------------------------------------------------------
// Static generation
// ---------------------------------------------------------------------------

export function generateStaticParams() {
  return getProductSlugs().map((slug) => ({ slug }));
}

type Params = Promise<{ slug: string }>;

export async function generateMetadata({
  params,
}: {
  params: Params;
}): Promise<Metadata> {
  const { slug } = await params;
  const product = getProduct(slug);
  const url = `${site.url}/shop/${product.slug}`;

  return {
    title: product.name,
    description: product.description,
    alternates: { canonical: url },
    openGraph: {
      title: `${product.name} · ${site.name}`,
      description: product.description,
      url,
      type: "website",
      images: [{ url: product.image.src, alt: product.image.alt }],
    },
    twitter: {
      card: "summary_large_image",
      title: `${product.name} · ${site.name}`,
      description: product.description,
      images: [product.image.src],
    },
  };
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default async function ProductDetailPage({
  params,
}: {
  params: Params;
}) {
  const { slug } = await params;
  const product = getProduct(slug);
  const collection = collections.find((c) => c.slug === product.collection);
  const delivery = getDeliveryEstimate(product);
  const related = getRelatedProducts(product.slug, 3);

  /**
   * JSON-LD Product — surfaces price, currency and availability to Google
   * for rich-result eligibility. The `Offer` structure mirrors what a
   * future backend `GET /api/products/{slug}` will return.
   */
  const productSchema = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    description: product.description,
    image: product.image.src,
    category: product.category,
    brand: { "@type": "Brand", name: site.name },
    offers: {
      "@type": "Offer",
      url: `${site.url}/shop/${product.slug}`,
      priceCurrency: "NGN",
      price: product.price,
      availability: product.available
        ? "https://schema.org/InStock"
        : "https://schema.org/PreOrder",
      itemCondition: "https://schema.org/NewCondition",
      seller: { "@type": "Organization", name: site.name },
    },
  };

  return (
    <>
      <script
        type="application/ld+json"
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{ __html: JSON.stringify(productSchema) }}
      />

      {/* Breadcrumb */}
      <nav
        aria-label="Breadcrumb"
        className="bg-ivory border-b border-ink/10"
      >
        <ol className="container flex flex-wrap items-center gap-2 py-5 text-xs uppercase tracking-widest text-stone-500">
          <li>
            <Link href="/" className="hover:text-ink transition-colors">
              Home
            </Link>
          </li>
          <ChevronRight size={12} aria-hidden />
          <li>
            <Link href="/shop" className="hover:text-ink transition-colors">
              Shop
            </Link>
          </li>
          <ChevronRight size={12} aria-hidden />
          <li>
            <Link
              href={`/shop?category=${product.category}`}
              className="hover:text-ink transition-colors"
            >
              {product.category}
            </Link>
          </li>
          <ChevronRight size={12} aria-hidden />
          <li aria-current="page" className="text-ink">
            {product.name}
          </li>
        </ol>
      </nav>

      {/* Main product view */}
      <section
        aria-label={product.name}
        className="pt-14 md:pt-20 pb-24 md:pb-32 bg-ivory"
      >
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-start">
          {/* Image */}
          <Reveal className="lg:col-span-7">
            <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
              <Image
                src={product.image.src}
                alt={`${product.name} — ${product.image.alt}`}
                fill
                priority
                fetchPriority="high"
                sizes="(min-width: 1024px) 55vw, 100vw"
                quality={90}
                className="object-cover"
              />
            </div>
          </Reveal>

          {/* Editorial meta */}
          <Reveal className="lg:col-span-5 lg:pl-4">
            <p className="eyebrow text-stone-500">
              <span className="rule inline-block align-middle mr-3" />
              {product.category}
              {collection && (
                <>
                  {" "}
                  ·{" "}
                  <Link
                    href={`/collections/${collection.slug}`}
                    className="hover:text-ink transition-colors"
                  >
                    {collection.name}
                  </Link>
                </>
              )}
            </p>

            <h1 className="mt-5 font-display text-4xl md:text-5xl lg:text-6xl leading-[1.02] tracking-tightest">
              {product.name}
            </h1>

            <div className="mt-6 flex items-baseline gap-4">
              <p className="font-display text-3xl tracking-tight text-ink">
                {product.available ? formatNaira(product.price) : "By Enquiry"}
              </p>
              {product.badge && (
                <span className="eyebrow bg-ink text-ivory px-3 py-1.5">
                  {product.badge}
                </span>
              )}
            </div>

            <p className="mt-6 text-stone-700 leading-relaxed text-lg max-w-lg">
              {product.description}
            </p>

            {/* Meta rail — fabric, construction, care, delivery */}
            <dl className="mt-10 divide-y divide-ink/10 border-y border-ink/10">
              {collection && (
                <MetaRow label="Fabric" value={collection.fabric} />
              )}
              <MetaRow label="Construction" value={product.construction} />
              <MetaRow label="Care" value={product.care} />
              <MetaRow label="Delivery" value={delivery} />
            </dl>

            <ProductActions product={product} />

            <SizeGuide category={product.category} />

            <p className="mt-10 text-xs text-stone-500 max-w-md">
              Every piece can also be commissioned to your measurements. Book a
              fitting or write to the atelier for a private consultation.
            </p>
          </Reveal>
        </div>
      </section>

      {/* Related pieces */}
      {related.length > 0 && (
        <section
          aria-label="You may also like"
          className="py-24 md:py-32 bg-stone-50 border-t border-ink/10"
        >
          <div className="container">
            <SectionHeader
              eyebrow="Continue browsing"
              title="You may also like."
              description="Pieces from the same collection — cut in the same rhythm, ready to layer."
            />

            <Stagger className="mt-16 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-16">
              {related.map((r) => (
                <StaggerItem key={r.slug}>
                  <ProductCard product={r} />
                </StaggerItem>
              ))}
            </Stagger>
          </div>
        </section>
      )}

      <CTASection
        eyebrow="Book a Fitting"
        title="Wear it to measure."
        body="Visit our Lagos atelier or arrange a private video consultation. Every ready-to-wear piece can be recut to your measurements, and every bespoke commission begins with a conversation."
        primaryLabel="Book a Fitting"
        primaryHref={`/contact?product=${product.slug}`}
        secondaryLabel="Explore the Collection"
        secondaryHref={collection ? `/collections/${collection.slug}` : "/collections"}
      />
    </>
  );
}

function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid grid-cols-3 gap-6 py-4">
      <dt className="eyebrow text-stone-500">{label}</dt>
      <dd className="col-span-2 text-ink">{value}</dd>
    </div>
  );
}
