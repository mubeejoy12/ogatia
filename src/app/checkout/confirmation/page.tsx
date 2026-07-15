import type { Metadata } from "next";
import { Suspense } from "react";
import { PageHeader } from "@/components/sections/PageHeader";
import { ConfirmationView } from "@/features/checkout/ConfirmationView";
import { site } from "@/lib/site";

/**
 * Confirmation is personal + transient — never indexed, never in the sitemap.
 * The order data lives in the customer's own localStorage.
 */
export const metadata: Metadata = {
  title: "Order confirmation",
  description: "Your Eazi Cut commission has been received.",
  alternates: { canonical: `${site.url}/checkout/confirmation` },
  robots: { index: false, follow: false },
};

export default function ConfirmationPage() {
  return (
    <>
      <PageHeader
        eyebrow="Confirmation"
        title="Your commission has been received."
        intro="A senior member of the atelier will reach out within one business day to confirm the details of your order."
      />

      <section aria-label="Order confirmation" className="pb-24 md:pb-32 bg-ivory">
        <div className="container">
          <Suspense fallback={<div aria-hidden className="min-h-[50vh]" />}>
            <ConfirmationView />
          </Suspense>
        </div>
      </section>
    </>
  );
}
