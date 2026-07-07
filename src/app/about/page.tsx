import type { Metadata } from "next";
import Image from "next/image";
import { PageHeader } from "@/components/sections/PageHeader";
import { CTASection } from "@/components/sections/CTASection";
import { Reveal, Stagger, StaggerItem } from "@/components/motion/Reveal";

export const metadata: Metadata = {
  title: "The Atelier",
  description:
    "Inside the Eazi Cut atelier — a Lagos house dedicated to luxury menswear, bespoke tailoring, and the modern African gentleman.",
};

const values = [
  {
    title: "Quiet luxury",
    body: "We believe a garment should be felt before it is noticed. Proportion, cloth, and finish over logo.",
  },
  {
    title: "African modernity",
    body: "We make pieces that are unmistakably African and unmistakably contemporary — without caricature.",
  },
  {
    title: "Patient craft",
    body: "A bespoke commission takes weeks for a reason. We do not rush what should be worn for a decade.",
  },
];

export default function AboutPage() {
  return (
    <>
      <PageHeader
        eyebrow="The Atelier"
        title="A house for the modern African gentleman."
        intro="Eazi Cut was founded in Lagos to bring the discipline of European tailoring and the soul of African craft under one roof. We dress executives, grooms, and creatives across three continents — quietly, precisely, and with the patience luxury demands."
      />

      <section className="pb-24 md:pb-32 bg-ivory">
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
          <Reveal className="lg:col-span-7 relative aspect-[4/5] overflow-hidden">
            <Image
              src="https://images.unsplash.com/photo-1542327897-d73f4005b533?auto=format&fit=crop&w=1800&q=85"
              alt="Master tailor at the cutting table inside the Lagos atelier"
              fill
              sizes="(min-width: 1024px) 58vw, 100vw"
              className="object-cover"
            />
          </Reveal>
          <Reveal className="lg:col-span-5 lg:pt-12">
            <p className="eyebrow">
              <span className="rule inline-block align-middle mr-3" />
              Our Story
            </p>
            <h2 className="mt-5 font-display text-4xl md:text-5xl leading-[1.05] tracking-tightest">
              Founded on a quiet idea.
            </h2>
            <p className="mt-6 text-stone-700 leading-relaxed">
              In a market crowded with fast tailoring and louder labels, we set
              out to build a house in the older tradition — one that measures
              by hand, sources by reputation, and finishes by feel.
            </p>
            <p className="mt-6 text-stone-700 leading-relaxed">
              Our clients are professionals, executives, and grooms who want
              clothes that hold their shape over decades — not seasons.
            </p>
          </Reveal>
        </div>
      </section>

      <section className="py-24 md:py-32 bg-stone-50">
        <div className="container">
          <Reveal>
            <p className="eyebrow">
              <span className="rule inline-block align-middle mr-3" />
              Principles
            </p>
            <h2 className="mt-5 font-display text-4xl md:text-5xl lg:text-6xl tracking-tightest leading-[1.05] max-w-3xl">
              Three principles, kept stubbornly.
            </h2>
          </Reveal>

          <Stagger className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-10">
            {values.map((v, i) => (
              <StaggerItem key={v.title} className="border-t border-ink/20 pt-8">
                <p className="font-display text-gold text-sm">
                  {String(i + 1).padStart(2, "0")}
                </p>
                <h3 className="mt-4 font-display text-2xl tracking-tightest">
                  {v.title}
                </h3>
                <p className="mt-4 text-stone-700 leading-relaxed">{v.body}</p>
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </section>

      <section className="py-24 md:py-32 bg-ivory">
        <div className="container grid grid-cols-1 lg:grid-cols-2 gap-16">
          <Reveal>
            <p className="eyebrow">
              <span className="rule inline-block align-middle mr-3" />
              Service
            </p>
            <h2 className="mt-5 font-display text-4xl md:text-5xl leading-[1.05] tracking-tightest">
              For Lagos. For the diaspora.
            </h2>
            <p className="mt-6 text-stone-700 leading-relaxed max-w-md">
              We see clients in person at our Lagos atelier by appointment, and
              by private video consultation for diaspora gentlemen in London,
              Houston, Toronto and Dubai. Garments ship insured, worldwide.
            </p>
          </Reveal>

          <Stagger className="grid grid-cols-2 gap-y-12 gap-x-8">
            {[
              { k: "1,400+", v: "Commissions delivered" },
              { k: "24", v: "Points of measurement" },
              { k: "4–6", v: "Weeks per bespoke commission" },
              { k: "3", v: "Continents served" },
            ].map((s) => (
              <StaggerItem key={s.v}>
                <p className="font-display text-5xl md:text-6xl text-ink tracking-tightest">
                  {s.k}
                </p>
                <p className="mt-3 eyebrow">{s.v}</p>
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </section>

      <CTASection
        title="Visit the atelier."
        body="By appointment, Tuesday to Saturday. Walk into a quiet room, leave with a wardrobe in motion."
        primaryLabel="Request an Appointment"
      />
    </>
  );
}
