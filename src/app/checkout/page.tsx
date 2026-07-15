import type { Metadata } from "next";
import { PageHeader } from "@/components/sections/PageHeader";
import { CheckoutView } from "@/features/checkout/CheckoutView";
import { site } from "@/lib/site";

/**
 * Checkout is personal + transient — never indexed, never in the sitemap.
 */
export const metadata: Metadata = {
  title: "Checkout",
  description: "Complete your Eazi Cut order.",
  alternates: { canonical: `${site.url}/checkout` },
  robots: { index: false, follow: false },
};

export default function CheckoutPage() {
  return (
    <>
      <PageHeader
        eyebrow="Checkout"
        title="Complete your order."
        intro="Shipping details first — payment in the next step. A senior member of the atelier team confirms every commission before it leaves Lagos."
      />

      <section aria-label="Checkout" className="pb-24 md:pb-32 bg-ivory">
        <div className="container">
          <CheckoutView />
        </div>
      </section>
    </>
  );
}
