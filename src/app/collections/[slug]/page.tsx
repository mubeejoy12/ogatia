import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { ArrowUpRight, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/sections/PageHeader";
import { SectionHeader } from "@/components/sections/SectionHeader";
import { CollectionCard } from "@/components/sections/CollectionCard";
import { CTASection } from "@/components/sections/CTASection";
import { Reveal, Stagger, StaggerItem } from "@/components/motion/Reveal";
import {
  getCollection,
  getCollectionSlugs,
  getRelatedCollections,
} from "@/lib/data/collections";
import { site } from "@/lib/site";

// ---------------------------------------------------------------------------
// Static generation
// ---------------------------------------------------------------------------

export function generateStaticParams() {
  return getCollectionSlugs().map((slug) => ({ slug }));
}

/**
 * Fully typed URL params. Next 15 makes params async — we await it.
 */
type Params = Promise<{ slug: string }>;

export async function generateMetadata({
  params,
}: {
  params: Params;
}): Promise<Metadata> {
  const { slug } = await params;
  const c = getCollection(slug);
  const url = `${site.url}/collections/${c.slug}`;

  return {
    title: c.name,
    description: c.description,
    alternates: { canonical: url },
    openGraph: {
      title: `${c.name} · ${site.name}`,
      description: c.description,
      url,
      type: "article",
      images: [{ url: c.image.src, alt: c.image.alt }],
    },
    twitter: {
      card: "summary_large_image",
      title: `${c.name} · ${site.name}`,
      description: c.description,
      images: [c.image.src],
    },
  };
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default async function CollectionDetailPage({
  params,
}: {
  params: Params;
}) {
  const { slug } = await params;
  const c = getCollection(slug);
  const related = getRelatedCollections(c.slug, 2);

  /**
   * Ticket 003 (Shop) will live at `/shop`. Filtering by collection uses the
   * `collection` query parameter — the URL that shop query filters generate.
   * Set here so this page is ready the moment Shop lands.
   */
  const shopHref = `/shop?collection=${c.slug}`;

  return (
    <>
      <PageHeader
        eyebrow={`Collection · ${c.category}`}
        title={c.name}
        intro={c.tagline}
      />

      {/* Editorial hero image + meta rail */}
      <section
        aria-label={`${c.name} — overview`}
        className="pb-24 md:pb-32 bg-ivory"
      >
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-start">
          <Reveal className="lg:col-span-7">
            <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
              <Image
                src={c.image.src}
                alt={c.image.alt}
                fill
                priority
                fetchPriority="high"
                sizes="(min-width: 1024px) 55vw, 100vw"
                quality={90}
                className="object-cover"
              />
            </div>
          </Reveal>

          <Reveal className="lg:col-span-5 lg:pl-6">
            <p className="eyebrow">
              <span className="rule inline-block align-middle mr-3" />
              The Collection
            </p>
            <div className="mt-6 space-y-6 text-stone-700 text-lg leading-relaxed">
              {c.story.map((paragraph, i) => (
                <p key={i}>{paragraph}</p>
              ))}
            </div>

            <dl className="mt-12 space-y-6 border-t border-ink/10 pt-8">
              <div className="grid grid-cols-3 gap-6">
                <dt className="eyebrow text-stone-500">Category</dt>
                <dd className="col-span-2 text-ink">{c.category}</dd>
              </div>
              <div className="grid grid-cols-3 gap-6">
                <dt className="eyebrow text-stone-500">Fabric</dt>
                <dd className="col-span-2 text-ink">{c.fabric}</dd>
              </div>
              <div className="grid grid-cols-3 gap-6">
                <dt className="eyebrow text-stone-500">Pieces</dt>
                <dd className="col-span-2 text-ink">
                  {c.pieces} in this collection
                </dd>
              </div>
              {c.startingPrice && (
                <div className="grid grid-cols-3 gap-6">
                  <dt className="eyebrow text-stone-500">From</dt>
                  <dd className="col-span-2 text-ink font-display text-2xl tracking-tightest">
                    {c.startingPrice}
                  </dd>
                </div>
              )}
            </dl>

            <div className="mt-12 flex flex-col sm:flex-row gap-4">
              <Button asChild variant="primary" size="lg">
                <Link href={shopHref}>Shop the Collection</Link>
              </Button>
              <Button asChild variant="secondary" size="lg">
                <Link href={`/contact?collection=${c.slug}`}>
                  Commission Bespoke
                </Link>
              </Button>
            </div>
            <p className="mt-4 text-xs text-stone-500 max-w-sm">
              Ready-to-wear ships worldwide from Lagos. Bespoke commissions
              begin with a private consultation.
            </p>
          </Reveal>
        </div>
      </section>

      {/* Signature pieces */}
      <section
        aria-label={`${c.name} — signature pieces`}
        className="py-24 md:py-32 bg-stone-50"
      >
        <div className="container">
          <SectionHeader
            eyebrow="Signature pieces"
            title="The wardrobe within the wardrobe."
            description="A curated set of pieces at the heart of the collection. Each can be commissioned, altered, or ordered ready-to-wear."
          />

          <Stagger className="mt-16 grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            {c.signaturePieces.map((piece) => (
              <StaggerItem key={piece}>
                <div className="flex items-center gap-4 border-t border-ink/10 py-6">
                  <Check
                    className="shrink-0 text-gold"
                    size={18}
                    aria-hidden
                  />
                  <p className="font-display text-xl md:text-2xl tracking-tightest">
                    {piece}
                  </p>
                </div>
              </StaggerItem>
            ))}
          </Stagger>

          <Reveal className="mt-16 flex flex-col sm:flex-row items-start sm:items-center gap-6 justify-between border-t border-ink/10 pt-10">
            <p className="text-stone-700 max-w-xl leading-relaxed">
              Every piece is available in the atelier's full fabric library and
              can be adjusted to your measurements. Ready-to-wear stock rotates
              quarterly.
            </p>
            <Link
              href={shopHref}
              className="inline-flex items-center gap-2 uppercase tracking-widest text-[11px] border-b border-ink pb-1 hover:gap-3 transition-all"
            >
              Shop all pieces <ArrowUpRight size={14} />
            </Link>
          </Reveal>
        </div>
      </section>

      {/* Related collections */}
      {related.length > 0 && (
        <section
          aria-label="Other collections"
          className="py-24 md:py-32 bg-ivory"
        >
          <div className="container">
            <SectionHeader
              eyebrow="Continue browsing"
              title="Other collections from the house."
            />

            <Stagger className="mt-16 grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-16">
              {related.map((r) => (
                <StaggerItem key={r.slug}>
                  <CollectionCard collection={r} />
                </StaggerItem>
              ))}
            </Stagger>
          </div>
        </section>
      )}

      <CTASection
        eyebrow="Book a Fitting"
        title="Wear this collection to measure."
        body="Visit the Lagos atelier or book a private video consultation. Each commission begins with a conversation, and every fabric in the library is available on request."
        primaryLabel="Book a Fitting"
        primaryHref={`/contact?collection=${c.slug}`}
        secondaryLabel="See in Lookbook"
        secondaryHref="/lookbook"
      />
    </>
  );
}
