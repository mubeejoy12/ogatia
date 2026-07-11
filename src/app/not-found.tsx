import Link from "next/link";
import type { Metadata } from "next";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = {
  title: "Page not found",
  description:
    "The page you were looking for has been moved or does not exist.",
  robots: { index: false, follow: false },
};

export default function NotFound() {
  return (
    <section
      className="min-h-[80vh] flex items-center justify-center bg-ivory py-24 md:py-32"
      aria-labelledby="not-found-title"
    >
      <div className="container max-w-2xl text-center">
        <p className="eyebrow">
          <span className="rule inline-block align-middle mr-3" />
          Four · Zero · Four
          <span className="rule inline-block align-middle ml-3" />
        </p>
        <h1
          id="not-found-title"
          className="mt-8 font-display tracking-tightest text-[clamp(2.75rem,7vw,5rem)] leading-[0.98]"
        >
          The page you were looking for
          <br />
          <span className="italic text-gold-deep">no longer resides here.</span>
        </h1>
        <p className="mt-8 text-stone-700 text-lg leading-relaxed max-w-lg mx-auto">
          Every collection, every commission has its own place. Let us return
          you to the atelier.
        </p>
        <div className="mt-12 flex flex-col sm:flex-row gap-4 justify-center">
          <Button asChild variant="primary" size="lg">
            <Link href="/">Return Home</Link>
          </Button>
          <Button asChild variant="secondary" size="lg">
            <Link href="/contact">Speak With Us</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
