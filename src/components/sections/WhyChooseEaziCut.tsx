import Image from "next/image";
import { Reveal, Stagger, StaggerItem } from "@/components/motion/Reveal";
import { assets } from "@/lib/assets";

const pillars = [
  {
    title: "Master Craftsmanship",
    body: "Garments hand-cut and assembled by senior tailors with decades at European and Nigerian houses.",
  },
  {
    title: "World-Class Cloth",
    body: "An in-house library of Italian wools, Egyptian cottons, and French linens — sourced from the same mills as Savile Row.",
  },
  {
    title: "Twenty-Four Point Fit",
    body: "Measurements taken by hand and preserved digitally, so every future commission begins already half-finished.",
  },
  {
    title: "Discreet Worldwide Delivery",
    body: "Insured shipping in signature ivory packaging — from Lagos to London, Houston, or Dubai.",
  },
];

export function WhyChooseEaziCut() {
  return (
    <section
      aria-label="Why choose Eazi Cut"
      className="bg-ink text-ivory py-24 md:py-32 overflow-hidden"
    >
      <div className="container grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-20">
        <Reveal className="lg:col-span-5">
          <p className="eyebrow text-ivory/60">
            <span className="rule inline-block align-middle mr-3 bg-ivory/40" />
            Why Eazi Cut
          </p>
          <h2 className="mt-5 font-display text-4xl md:text-5xl lg:text-6xl leading-[1.05] tracking-tightest">
            A house, not a shop.
          </h2>
          <p className="mt-6 text-ivory/70 leading-relaxed text-lg max-w-md">
            We do not chase trend. We build wardrobes that will be worn — and
            cherished — a decade from now. Four quiet principles guide every
            garment we make.
          </p>

          <div className="relative mt-12 aspect-[4/5] max-w-sm overflow-hidden">
            <Image
              src={assets.whyChoose.craft.src}
              alt={assets.whyChoose.craft.alt}
              fill
              sizes="(min-width: 1024px) 400px, 100vw"
              className="object-cover"
            />
          </div>
        </Reveal>

        <Stagger className="lg:col-span-7 grid grid-cols-1 sm:grid-cols-2 gap-x-10 gap-y-12 lg:pt-24">
          {pillars.map((p, i) => (
            <StaggerItem key={p.title}>
              <p className="font-display text-gold text-sm tracking-widest">
                {String(i + 1).padStart(2, "0")}
              </p>
              <h3 className="mt-4 font-display text-2xl tracking-tightest">
                {p.title}
              </h3>
              <p className="mt-3 text-ivory/70 leading-relaxed">{p.body}</p>
              <div className="mt-6 h-px w-10 bg-gold" />
            </StaggerItem>
          ))}
        </Stagger>
      </div>
    </section>
  );
}
