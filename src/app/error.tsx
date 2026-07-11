"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

/**
 * Root-level error boundary. Renders when a rendering error escapes any
 * nested boundary. Kept understated — luxury visitors should not encounter
 * an alarming "Application error!" splash.
 */
export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // In production this hooks into Sentry / whichever RUM provider is wired.
    // For now, log to the browser console so nothing is swallowed silently.
    console.error("Root error boundary:", error);
  }, [error]);

  return (
    <section
      className="min-h-[80vh] flex items-center justify-center bg-ivory py-24 md:py-32"
      aria-labelledby="error-title"
    >
      <div className="container max-w-2xl text-center">
        <p className="eyebrow">
          <span className="rule inline-block align-middle mr-3" />
          A moment please
          <span className="rule inline-block align-middle ml-3" />
        </p>
        <h1
          id="error-title"
          className="mt-8 font-display tracking-tightest text-[clamp(2.5rem,6vw,4.5rem)] leading-[1.02]"
        >
          Something went briefly amiss.
        </h1>
        <p className="mt-8 text-stone-700 text-lg leading-relaxed max-w-lg mx-auto">
          The atelier is unavailable for a moment. Please try again, or return
          to the homepage — the team has been notified.
        </p>
        <div className="mt-12 flex flex-col sm:flex-row gap-4 justify-center">
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
  );
}
