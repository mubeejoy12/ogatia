import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/sections/PageHeader";

/**
 * Branded not-found for /shop/[slug] — fires when {@code fetchProductBySlug}
 * returns 404 (missing product, deleted product, or a hand-typed slug that
 * doesn't resolve). Keeps the customer inside the site and offers two
 * natural next steps.
 */
export const metadata: Metadata = {
  title: "Piece not found",
  robots: { index: false, follow: false },
};

export default function ProductNotFound() {
  return (
    <>
      <PageHeader eyebrow="Shop" title="This piece is no longer here." />
      <section aria-label="Not found" className="pb-24 md:pb-32 bg-ivory">
        <div className="container max-w-xl text-center mt-12">
          <p className="text-stone-700 leading-relaxed">
            The piece you were looking for has been removed from the catalogue,
            or its link is out of date. Return to the shop to see what is
            currently available — or write to the atelier to ask after a
            specific piece.
          </p>
          <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
            <Button asChild variant="primary" size="lg">
              <Link href="/shop">Return to Shop</Link>
            </Button>
            <Button asChild variant="secondary" size="lg">
              <Link href="/contact">Write to the Atelier</Link>
            </Button>
          </div>
        </div>
      </section>
    </>
  );
}
