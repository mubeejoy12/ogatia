import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { collections } from "@/lib/data/collections";
import { SectionHeader } from "./SectionHeader";
import { CollectionCard } from "./CollectionCard";
import { Stagger, StaggerItem } from "@/components/motion/Reveal";

export function FeaturedCollections({ limit = 4 }: { limit?: number }) {
  const items = collections.slice(0, limit);

  return (
    <section className="py-24 md:py-32 bg-ivory">
      <div className="container">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-8">
          <SectionHeader
            eyebrow="Collections"
            title="A wardrobe of considered pieces."
            description="Each collection is conceived as a complete wardrobe — pieces that converse with one another, from boardroom to ceremony."
          />
          <Link
            href="/collections"
            className="group inline-flex items-center gap-2 self-start md:self-end uppercase tracking-widest text-[11px] border-b border-ink pb-2 hover:gap-3 transition-all"
          >
            View all collections <ArrowUpRight size={14} />
          </Link>
        </div>

        <Stagger className="mt-16 grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-16">
          {items.map((c, i) => (
            <StaggerItem key={c.slug}>
              <CollectionCard collection={c} index={i} />
            </StaggerItem>
          ))}
        </Stagger>
      </div>
    </section>
  );
}
