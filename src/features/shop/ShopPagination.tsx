"use client";

import { useCallback } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Prev / next pager for the /shop grid.
 *
 * Reads the URL as the single source of truth (so it stays in step with
 * the server-side fetch and the toolbar). Updates the `page` query param
 * via `router.replace` — the server component re-runs the fetch and passes
 * new data downstream.
 *
 * `page` is 1-indexed in the URL (customer-facing) but the parent may
 * emit a 0-indexed number; both sides are converted at the boundary.
 */
export function ShopPagination({
  page,
  totalPages,
}: {
  /** 1-indexed page number currently on screen. */
  page: number;
  /** Total pages available for the current query. */
  totalPages: number;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const go = useCallback(
    (next: number) => {
      const params = new URLSearchParams(searchParams.toString());
      if (next <= 1) params.delete("page");
      else params.set("page", String(next));
      const qs = params.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: true });
    },
    [pathname, router, searchParams]
  );

  if (totalPages <= 1) return null;

  const hasPrev = page > 1;
  const hasNext = page < totalPages;

  return (
    <nav
      aria-label="Product pagination"
      className="mt-20 flex items-center justify-between border-t border-ink/10 pt-8"
    >
      <PagerButton
        disabled={!hasPrev}
        onClick={() => go(page - 1)}
        aria-label="Previous page"
      >
        <ChevronLeft size={16} aria-hidden />
        <span>Previous</span>
      </PagerButton>

      <p
        aria-live="polite"
        className="eyebrow text-stone-500 text-[10px]"
      >
        Page {page} of {totalPages}
      </p>

      <PagerButton
        disabled={!hasNext}
        onClick={() => go(page + 1)}
        aria-label="Next page"
      >
        <span>Next</span>
        <ChevronRight size={16} aria-hidden />
      </PagerButton>
    </nav>
  );
}

function PagerButton({
  disabled,
  onClick,
  children,
  ...aria
}: {
  disabled: boolean;
  onClick: () => void;
  children: React.ReactNode;
} & React.AriaAttributes) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "inline-flex items-center gap-2 uppercase tracking-widest text-[11px] py-2 px-3",
        "focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory",
        disabled
          ? "text-stone-300 cursor-not-allowed"
          : "text-ink hover:text-gold-deep transition-colors"
      )}
      {...aria}
    >
      {children}
    </button>
  );
}
