import { testimonials } from "@/lib/data/testimonials";
import { SectionHeader } from "./SectionHeader";
import { Stagger, StaggerItem } from "@/components/motion/Reveal";

export function Testimonials() {
  return (
    <section className="py-24 md:py-32 bg-ivory">
      <div className="container">
        <SectionHeader
          eyebrow="Clientele"
          title="Worn by gentlemen across three continents."
          align="center"
        />

        <Stagger className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8 md:gap-6">
          {testimonials.map((t) => (
            <StaggerItem
              key={t.name}
              className="border border-ink/10 p-10 bg-stone-50/60 flex flex-col"
            >
              <span
                aria-hidden
                className="font-display text-gold text-6xl leading-none"
              >
                &ldquo;
              </span>
              <blockquote className="mt-6 font-display text-2xl leading-snug tracking-tightest text-ink">
                {t.quote}
              </blockquote>
              <div className="mt-auto pt-10">
                <div className="h-px w-10 bg-ink/30" />
                <p className="mt-5 text-sm font-medium">{t.name}</p>
                <p className="text-stone-500 text-xs uppercase tracking-widest mt-1">
                  {t.role} · {t.city}
                </p>
              </div>
            </StaggerItem>
          ))}
        </Stagger>
      </div>
    </section>
  );
}
