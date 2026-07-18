"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/sections/PageHeader";

/**
 * Route-level error boundary for /shop.
 *
 * Overrides the root error.tsx with copy tuned to the shopping context
 * — "the collection is momentarily unavailable" reads far better than
 * a generic "something went wrong" for a customer holding a wishlist.
 */
export default function ShopError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Shop error boundary:", error);
  }, [error]);

  return (
    <>
      <PageHeader eyebrow="Shop" title="A moment please." />
      <section aria-label="Error" className="pb-24 md:pb-32 bg-ivory">
        <div className="container max-w-xl text-center mt-12">
          <p className="text-stone-700 leading-relaxed">
            The catalogue is momentarily unavailable. Please try again in a few
            seconds — or return to the atelier home page.
          </p>
          <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
            <Button variant="primary" size="lg" onClick={reset}>
              Try again
            </Button>
            <Button asChild variant="secondary" size="lg">
              <Link href="/">Return Home</Link>
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
