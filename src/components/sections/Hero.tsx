"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { assets } from "@/lib/assets";

export function Hero() {
  const hero = assets.hero.home;
  return (
    <section className="relative h-[100svh] min-h-[640px] w-full overflow-hidden bg-ink text-ivory">
      <Image
        src={hero.src}
        alt={hero.alt}
        fill
        priority
        sizes="100vw"
        className="object-cover object-center opacity-70"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-ink/60 via-ink/20 to-ink/80" />

      <div className="relative h-full container flex flex-col justify-end pb-20 md:pb-28">
        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
          className="eyebrow text-ivory/70"
        >
          Bespoke · Ready-to-Wear · Wedding
        </motion.p>

        <motion.h1
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.15, ease: [0.22, 1, 0.36, 1] }}
          className="mt-6 font-display tracking-tightest text-[clamp(2.75rem,7vw,6.5rem)] leading-[0.95] max-w-5xl"
        >
          The modern gentleman,
          <br />
          <span className="italic text-gold-soft">tailored without friction.</span>
        </motion.h1>

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
          className="mt-10 flex flex-col sm:flex-row gap-4 items-start sm:items-center"
        >
          <Button asChild variant="gold" size="lg">
            <Link href="/contact">Book a Fitting</Link>
          </Button>
          <Button asChild variant="outlineIvory" size="lg">
            <Link href="/collections">Explore Collections</Link>
          </Button>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 1.2, delay: 0.8 }}
          className="hidden md:flex absolute right-10 bottom-28 flex-col items-end gap-4 max-w-xs text-right"
        >
          <div className="rule bg-ivory/60 self-end" />
          <p className="text-ivory/70 text-sm leading-relaxed">
            An atelier in Lagos. A clientele across three continents. A practice
            of luxury menswear refined for the modern African gentleman.
          </p>
        </motion.div>
      </div>
    </section>
  );
}
