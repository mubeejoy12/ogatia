import { testimonials } from "@/lib/data/testimonials";
import { SectionHeader } from "./SectionHeader";
import { Stagger, StaggerItem } from "@/components/motion/Reveal";

export function Testimonials() {
  return (
    <section className="py-24 md:py-32 bg-ivory" aria-label="Client voices">
      <div className="container">
        <SectionHeader
          eyebrow="Clientele"
          title="Worn by gentlemen across three continents."
          align="center"
        />

        <Stagger className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8 md:gap-6">
          {testimonials.map((t) => (
            <StaggerItem
              key={`${t.role}-${t.city}`}
              className="border border-ink/10 p-10 bg-stone-50/60 flex flex-col"
            >
              <figure className="flex flex-col h-full">
                <span
                  aria-hidden
                  className="font-display text-gold text-6xl leading-none"
                >
                  &ldquo;
                </span>
                <blockquote className="mt-6 font-display text-2xl leading-snug tracking-tightest text-ink">
                  {t.quote}
                </blockquote>
                <figcaption className="mt-auto pt-10">
                  <span
                    className="block h-px w-10 bg-ink/30"
                    aria-hidden
                  />
                  <p className="mt-5 text-xs uppercase tracking-widest text-stone-500">
                    {t.role} · {t.city}
                  </p>
                </figcaption>
              </figure>
            </StaggerItem>
          ))}
        </Stagger>
      </div>
    </section>
  );
}
