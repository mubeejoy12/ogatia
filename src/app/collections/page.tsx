import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { PageHeader } from "@/components/sections/PageHeader";
import { CTASection } from "@/components/sections/CTASection";
import { collections } from "@/lib/data/collections";
import { Reveal } from "@/components/motion/Reveal";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = {
  title: "Collections",
  description:
    "Bespoke, ready-to-wear, wedding and heritage collections from the Eazi Cut atelier in Lagos.",
};

export default function CollectionsPage() {
  return (
    <>
      <PageHeader
        eyebrow="Collections"
        title="Six wardrobes, one house."
        intro="Each collection is conceived as a complete wardrobe — from the boardroom to the ballroom, from the heritage agbada to the lightweight diaspora suit. Browse by season, by occasion, or by mood."
      />

      <section className="pb-24 md:pb-32 bg-ivory">
        <div className="container space-y-28 md:space-y-40">
          {collections.map((c, i) => {
            const reverse = i % 2 === 1;
            return (
              <article
                key={c.slug}
                id={c.slug}
                className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-center scroll-mt-32"
              >
                <Reveal
                  className={
                    reverse
                      ? "lg:col-span-7 lg:order-2"
                      : "lg:col-span-7"
                  }
                >
                  <div className="relative aspect-[4/5] overflow-hidden">
                    <Image
                      src={c.image.src}
                      alt={c.image.alt}
                      fill
                      sizes="(min-width: 1024px) 58vw, 100vw"
                      className="object-cover"
                    />
                  </div>
                </Reveal>

                <Reveal
                  className={
                    reverse
                      ? "lg:col-span-5 lg:order-1 lg:pr-8"
                      : "lg:col-span-5 lg:pl-8"
                  }
                >
                  <p className="eyebrow">
                    <span className="rule inline-block align-middle mr-3" />
                    {String(i + 1).padStart(2, "0")} · {c.category}
                  </p>
                  <h2 className="mt-5 font-display text-4xl md:text-5xl lg:text-6xl leading-[1.02] tracking-tightest">
                    {c.name}
                  </h2>
                  <p className="mt-6 text-stone-700 leading-relaxed text-lg">
                    {c.description}
                  </p>
                  <p className="mt-6 eyebrow text-stone-500">
                    {c.pieces} pieces in this collection
                  </p>
                  <div className="mt-10 flex flex-wrap gap-4">
                    <Button asChild variant="primary">
                      <Link href="/contact">Commission this collection</Link>
                    </Button>
                    <Link
                      href="/lookbook"
                      className="inline-flex items-center gap-2 uppercase tracking-widest text-[11px] border-b border-ink pb-1 hover:gap-3 transition-all"
                    >
                      See in lookbook <ArrowUpRight size={14} />
                    </Link>
                  </div>
                </Reveal>
              </article>
            );
          })}
        </div>
      </section>

      <CTASection
        eyebrow="Bespoke"
        title="Cannot find what you are looking for?"
        body="Every Eazi Cut commission can be reimagined to your brief. Bring a reference, a fabric, a memory — we build the rest."
        primaryLabel="Start a Commission"
      />
    </>
  );
}
