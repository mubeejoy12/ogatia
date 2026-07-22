import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ChevronRight } from "lucide-react";
import { CTASection } from "@/components/sections/CTASection";
import { SectionHeader } from "@/components/sections/SectionHeader";
import { Reveal, Stagger, StaggerItem } from "@/components/motion/Reveal";
import { ProductActions } from "@/features/shop/ProductActions";
import { ProductCard } from "@/components/shop/ProductCard";
import { SizeGuide } from "@/components/shop/SizeGuide";
import {
  fetchProductBySlug,
  fetchProducts,
} from "@/lib/api/products";
import { mapApiProduct } from "@/lib/api/adapters/product";
import { ApiNotFoundError } from "@/lib/api/errors";
import { getDeliveryEstimate } from "@/lib/delivery";
import { collections } from "@/lib/data/collections";
import { formatNaira } from "@/lib/format";
import { site } from "@/lib/site";
import type { Product } from "@/types/product";

// ---------------------------------------------------------------------------
// Dynamic rendering — the backend is the schema authority for products, so
// prerendering the full catalogue at build time would either couple the
// build to a running backend or drift out of sync. Every request re-fetches.
// ---------------------------------------------------------------------------

export const dynamic = "force-dynamic";

type Params = Promise<{ slug: string }>;

// ---------------------------------------------------------------------------
// Metadata
// ---------------------------------------------------------------------------

export async function generateMetadata({
  params,
}: {
  params: Params;
}): Promise<Metadata> {
  const { slug } = await params;
  try {
    const product = mapApiProduct(await fetchProductBySlug(slug));
    const url = `${site.url}/shop/${product.slug}`;
    const description = product.fullDescription ?? product.description;

    return {
      title: product.name,
      description,
      alternates: { canonical: url },
      openGraph: {
        title: `${product.name} · ${site.name}`,
        description,
        url,
        type: "website",
        images: product.image.src
          ? [{ url: product.image.src, alt: product.image.alt }]
          : undefined,
      },
      twitter: {
        card: "summary_large_image",
        title: `${product.name} · ${site.name}`,
        description,
        images: product.image.src ? [product.image.src] : undefined,
      },
    };
  } catch {
    // Missing product / backend unreachable — return safe minimal metadata
    // so the request still completes with a valid <head>. The page itself
    // will render the branded not-found or error boundary.
    return {
      title: "Product",
      robots: { index: false, follow: false },
    };
  }
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

  let product: Product;
  try {
    product = mapApiProduct(await fetchProductBySlug(slug));
  } catch (err) {
    if (err instanceof ApiNotFoundError) notFound();
    throw err;
  }

  const collection = collections.find((c) => c.slug === product.collection);
  const delivery = getDeliveryEstimate(product);
  const fabricLine = product.fabricType ?? collection?.fabric ?? null;
  const detailBody = product.fullDescription ?? product.description;

  // Related products — same collection, first 3 excluding self. Fetched
  // through the same API layer as the main product; failure here is
  // non-fatal (the strip simply doesn't render).
  const related = await fetchRelated(product);

  const productSchema = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    description: detailBody,
    image: product.image.src || undefined,
    sku: product.id,
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
              {product.image.src ? (
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
              ) : null}
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
              {detailBody}
            </p>

            {/* Meta rail — rows only render when the backend supplies a value */}
            <dl className="mt-10 divide-y divide-ink/10 border-y border-ink/10">
              <MetaRow label="Fabric" value={fabricLine} />
              <MetaRow label="Colour" value={product.color} />
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

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Meta row that hides itself when the backend has no value to show.
 * Empty strings, null, and undefined all suppress the row.
 */
function MetaRow({
  label,
  value,
}: {
  label: string;
  value: string | null | undefined;
}) {
  if (value === null || value === undefined || value === "") return null;
  return (
    <div className="grid grid-cols-3 gap-6 py-4">
      <dt className="eyebrow text-stone-500">{label}</dt>
      <dd className="col-span-2 text-ink">{value}</dd>
    </div>
  );
}

/**
 * Fetch up to 3 pieces from the same collection, excluding this one.
 * Failure is non-fatal — an empty list simply omits the "You may also
 * like" strip.
 */
async function fetchRelated(product: Product): Promise<Product[]> {
  try {
    const page = await fetchProducts({
      collection: product.collection,
      size: 6,
    });
    return page.content
      .map(mapApiProduct)
      .filter((p) => p.slug !== product.slug)
      .slice(0, 3);
  } catch {
    return [];
  }
}
