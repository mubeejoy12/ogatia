import Image from "next/image";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { formatNaira } from "@/lib/format";
import type { Product, ProductBadge } from "@/types/product";

/**
 * ProductCard — the canonical shop tile.
 *
 * Used on:
 *  - /shop grid (all products)
 *  - future PDP "related pieces" strip
 *  - future collection detail "shop the collection" section
 *
 * Content per DoD:
 *  · Image with editorial hover  ✓
 *  · Merchandising badge (data-driven)  ✓
 *  · Category eyebrow  ✓
 *  · Name  ✓
 *  · Price (or "Enquire" when unavailable)  ✓
 *  · Whole-card link to the PDP  ✓
 */
export function ProductCard({
  product,
  priority = false,
  className,
}: {
  product: Product;
  /** Marks the image as high-priority for above-the-fold cards. */
  priority?: boolean;
  className?: string;
}) {
  const priceLabel = product.available
    ? formatNaira(product.price)
    : "Enquire";

  const ariaLabel = product.available
    ? `${product.name} — ${formatNaira(product.price)}`
    : `${product.name} — enquire for availability`;

  return (
    <Link
      href={`/shop/${product.slug}`}
      aria-label={ariaLabel}
      className={cn(
        "group block focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory",
        className
      )}
    >
      <div className="relative aspect-[4/5] overflow-hidden bg-stone-100">
        <Image
          src={product.image.src}
          alt={`${product.name} — ${product.image.alt}`}
          fill
          sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
          priority={priority}
          className={cn(
            "object-cover transition-transform duration-1200 ease-editorial group-hover:scale-105",
            !product.available && "opacity-90"
          )}
        />

        {product.badge && <ProductBadgeChip badge={product.badge} />}

        {!product.available && (
          <span
            className="absolute bottom-4 left-4 eyebrow bg-ivory text-ink px-3 py-1.5"
            aria-hidden
          >
            By Enquiry
          </span>
        )}
      </div>

      <div className="mt-5 flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="eyebrow text-stone-500">{product.category}</p>
          <h3 className="mt-2 font-display text-xl md:text-2xl tracking-tightest leading-tight">
            {product.name}
          </h3>
          <p className="mt-2 text-stone-700 text-sm leading-relaxed max-w-xs">
            {product.description}
          </p>
        </div>
        <p
          className={cn(
            "shrink-0 font-display text-lg md:text-xl tracking-tight text-ink whitespace-nowrap",
            !product.available && "text-stone-500 italic"
          )}
        >
          {priceLabel}
        </p>
      </div>
    </Link>
  );
}

/**
 * The merchandising chip. Distinct visual treatment per badge so the
 * signal is legible at a glance — New is soft, Bespoke is house-gold,
 * Limited is ink for weight.
 */
function ProductBadgeChip({ badge }: { badge: ProductBadge }) {
  const styles: Record<ProductBadge, string> = {
    New: "bg-ivory text-ink",
    Bespoke: "bg-gold text-ivory",
    Limited: "bg-ink text-ivory",
  };

  return (
    <span
      className={cn(
        "absolute top-4 left-4 eyebrow px-3 py-1.5",
        styles[badge]
      )}
    >
      {badge}
    </span>
  );
}
