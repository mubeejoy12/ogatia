"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/sections/PageHeader";

/**
 * Route-level error boundary for /checkout.
 *
 * Payment errors are high-stakes — the copy is explicit that no charge
 * has been made, and points at the two safe next actions (retry or
 * back to bag).
 */
export default function CheckoutError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Checkout error boundary:", error);
  }, [error]);

  return (
    <>
      <PageHeader eyebrow="Checkout" title="Something went briefly amiss." />
      <section aria-label="Error" className="pb-24 md:pb-32 bg-ivory">
        <div className="container max-w-xl text-center mt-12">
          <p className="text-stone-700 leading-relaxed">
            No charge has been made. Please try again — or return to your bag
            and start the checkout afresh.
          </p>
          <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
            <Button variant="primary" size="lg" onClick={reset}>
              Try again
            </Button>
            <Button asChild variant="secondary" size="lg">
              <Link href="/cart">Return to Bag</Link>
            </Button>
          </div>
          {error.digest && (
            <p className="mt-10 text-stone-400 text-xs uppercase tracking-widest">
              Reference · {error.digest}
            </p>
          )}
        </div>
      </section>
    </>
  );
}
