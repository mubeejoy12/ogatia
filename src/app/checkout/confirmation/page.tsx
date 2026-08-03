"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

/**
 * Legacy redirect. The canonical order confirmation URL is now
 * {@code /orders/[reference]} (B006 Stage 5). This route stays to
 * catch bookmarks and in-flight tabs from the pre-B006 checkout
 * flow. On mount:
 *   * {@code ?ref=EAZI-…} → 302 to {@code /orders/EAZI-…}
 *   * no ref → 302 to {@code /account/orders}
 *
 * <p>Not indexed (parent metadata inherited from robots.txt +
 * the noindex on the previous version's Metadata).
 */
export default function LegacyConfirmationPage() {
  return (
    <Suspense fallback={<div aria-hidden className="min-h-[50vh]" />}>
      <LegacyConfirmationRedirect />
    </Suspense>
  );
}

function LegacyConfirmationRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const ref = searchParams.get("ref");
    if (ref) {
      router.replace(`/orders/${encodeURIComponent(ref)}`);
    } else {
      router.replace("/account/orders");
    }
  }, [router, searchParams]);

  return (
    <section className="mx-auto max-w-3xl px-6 py-24">
      <p className="text-ink/70">Redirecting to your order…</p>
    </section>
  );
}
