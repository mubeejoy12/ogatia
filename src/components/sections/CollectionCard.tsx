import Image from "next/image";
import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Collection } from "@/lib/data/collections";

/**
 * The canonical Collection card. Used on:
 *  - the Home page's Featured Collections grid
 *  - the /collections index grid
 *  - the "Related collections" strip on each detail page
 *
 * Card content (per Ticket 002 brief):
 *  · Hero image  ✓
 *  · Collection name  ✓
 *  · Short description  ✓
 *  · Number of pieces (dynamic-ready via `collection.pieces`)  ✓
 *  · Explicit CTA to explore the collection  ✓
 */
export function CollectionCard({
  collection,
  index,
  href,
  className,
  priority = false,
}: {
  collection: Collection;
  /** Optional numeric ordinal for the "01 · Bespoke" tag. */
  index?: number;
  /** Override the default detail-page URL. */
  href?: string;
  className?: string;
  /** Marks this image as high-priority (use on above-the-fold cards). */
  priority?: boolean;
}) {
  const target = href ?? `/collections/${collection.slug}`;
  const ordinal =
    typeof index === "number" ? String(index + 1).padStart(2, "0") : undefined;

  return (
    <Link
      href={target}
      aria-label={`Explore the ${collection.name} collection`}
      className={cn(
        "group block focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory",
        className
      )}
    >
      <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
        <Image
          src={collection.image.src}
          alt={collection.image.alt}
          fill
          sizes="(min-width: 1024px) 33vw, (min-width: 768px) 50vw, 100vw"
          priority={priority}
          className="object-cover transition-transform duration-1200 ease-editorial group-hover:scale-105"
        />
        <span className="absolute top-6 left-6 eyebrow text-ivory bg-ink/40 backdrop-blur-sm px-3 py-1.5">
          {ordinal ? `${ordinal} · ` : ""}
          {collection.category}
        </span>
      </div>

      <div className="mt-6 flex items-start justify-between gap-6">
        <div className="min-w-0">
          <h3 className="font-display text-2xl md:text-3xl tracking-tightest">
            {collection.name}
          </h3>
          <p className="mt-3 text-stone-700 max-w-md leading-relaxed">
            {collection.description}
          </p>
          <p className="mt-4 eyebrow text-stone-500">
            {collection.pieces} pieces
          </p>
        </div>
        <span
          aria-hidden
          className="shrink-0 mt-2 inline-flex items-center justify-center w-10 h-10 border border-ink/40 group-hover:bg-ink group-hover:text-ivory transition-colors duration-500"
        >
          <ArrowUpRight size={16} />
        </span>
      </div>

      <span className="mt-6 inline-flex items-center gap-2 uppercase tracking-widest text-[11px] border-b border-ink pb-1 group-hover:gap-3 transition-all">
        Explore the Collection
      </span>
    </Link>
  );
}
