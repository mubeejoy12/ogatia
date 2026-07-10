import Image from "next/image";
import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Collection } from "@/lib/data/collections";

export function CollectionCard({
  collection,
  index,
  href,
  className,
  priority = false,
}: {
  collection: Collection;
  index: number;
  href?: string;
  className?: string;
  priority?: boolean;
}) {
  const target = href ?? `/collections#${collection.slug}`;
  return (
    <Link
      href={target}
      aria-label={`${collection.name} collection`}
      className={cn("group block", className)}
    >
      <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
        <Image
          src={collection.image.src}
          alt={collection.image.alt}
          fill
          sizes="(min-width: 768px) 50vw, 100vw"
          priority={priority}
          className="object-cover transition-transform duration-1200 ease-editorial group-hover:scale-105"
        />
        <span className="absolute top-6 left-6 eyebrow text-ivory bg-ink/40 backdrop-blur-sm px-3 py-1.5">
          {String(index + 1).padStart(2, "0")} · {collection.category}
        </span>
      </div>
      <div className="mt-6 flex items-start justify-between gap-6">
        <div>
          <h3 className="font-display text-2xl md:text-3xl tracking-tightest">
            {collection.name}
          </h3>
          <p className="mt-3 text-stone-700 max-w-md leading-relaxed">
            {collection.description}
          </p>
        </div>
        <span
          aria-hidden
          className="shrink-0 mt-2 inline-flex items-center justify-center w-10 h-10 border border-ink/40 group-hover:bg-ink group-hover:text-ivory transition-colors duration-500"
        >
          <ArrowUpRight size={16} />
        </span>
      </div>
    </Link>
  );
}
