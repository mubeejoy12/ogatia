import Image from "next/image";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Reveal } from "@/components/motion/Reveal";
import { assets, type ImageAsset } from "@/lib/assets";

export function CTASection({
  eyebrow = "Begin Your Commission",
  title = "Your next garment begins with a conversation.",
  body = "Visit our Lagos atelier or book a private video consultation. Each commission is a single conversation away.",
  primaryHref = "/contact",
  primaryLabel = "Book a Fitting",
  secondaryHref = "/lookbook",
  secondaryLabel = "View the Lookbook",
  image = assets.cta.default,
}: {
  eyebrow?: string;
  title?: string;
  body?: string;
  primaryHref?: string;
  primaryLabel?: string;
  secondaryHref?: string;
  secondaryLabel?: string;
  image?: ImageAsset;
}) {
  return (
    <section className="relative isolate overflow-hidden bg-ink text-ivory">
      <Image
        src={image.src}
        alt=""
        fill
        sizes="100vw"
        className="object-cover object-center opacity-40"
        aria-hidden
      />
      <div className="absolute inset-0 bg-gradient-to-t from-ink via-ink/80 to-ink/40" />

      <div className="relative container py-28 md:py-40">
        <Reveal className="max-w-3xl">
          <p className="eyebrow text-ivory/60">
            <span className="rule inline-block align-middle mr-3 bg-ivory/40" />
            {eyebrow}
          </p>
          <h2 className="mt-6 font-display text-4xl md:text-6xl lg:text-7xl leading-[1.02] tracking-tightest">
            {title}
          </h2>
          <p className="mt-8 text-ivory/70 leading-relaxed text-lg max-w-xl">
            {body}
          </p>
          <div className="mt-12 flex flex-col sm:flex-row gap-4">
            <Button asChild variant="gold" size="lg">
              <Link href={primaryHref}>{primaryLabel}</Link>
            </Button>
            <Button asChild variant="outlineIvory" size="lg">
              <Link href={secondaryHref}>{secondaryLabel}</Link>
            </Button>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
