import type { Metadata } from "next";
import { PageHeader } from "@/components/sections/PageHeader";
import { CartView } from "@/features/cart/CartView";
import { site } from "@/lib/site";

/**
 * Cart is a personal, session-scoped view — deliberately excluded from
 * search-engine indexing. Not in sitemap.xml either.
 */
export const metadata: Metadata = {
  title: "The Bag",
  description: "Review the pieces in your bag before checkout.",
  alternates: { canonical: `${site.url}/cart` },
  robots: { index: false, follow: false },
};

export default function CartPage() {
  return (
    <>
      <PageHeader
        eyebrow="The Bag"
        title="Your selection."
        intro="Review your pieces below. Every commission remains editable up to the moment of checkout."
      />

      <section aria-label="Cart" className="pb-24 md:pb-32 bg-ivory">
        <div className="container">
          <CartView />
        </div>
      </section>
    </>
  );
}
