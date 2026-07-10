import { Reveal } from "@/components/motion/Reveal";

/**
 * Editorial statement placed between the Hero and the collection grid.
 *
 * Serves as a deliberate pause — the reader is asked to slow down before
 * being shown product. This is the tempo luxury houses use (Zegna, Brioni,
 * Boateng) — a moment of positioning before commerce.
 */
export function BrandStatement() {
  return (
    <section
      className="bg-ivory py-24 md:py-36"
      aria-label="Brand statement"
    >
      <div className="container">
        <Reveal className="mx-auto max-w-4xl text-center">
          <p className="eyebrow">
            <span className="rule inline-block align-middle mr-3" />
            The House
            <span className="rule inline-block align-middle ml-3" />
          </p>
          <p className="mt-10 font-display text-[clamp(1.75rem,3.4vw,3rem)] leading-[1.15] tracking-tightest text-ink">
            Every garment we make is cut for one gentleman. Cloth is chosen
            by hand, silhouette drawn to measure, finishing worked by tailors
            trained across three continents.
          </p>
          <p className="mt-8 text-stone-700 leading-relaxed text-lg max-w-2xl mx-auto">
            Eazi Cut is not a store. It is an atelier — a house of quiet
            craft, built for the modern African gentleman and his wardrobe.
          </p>
        </Reveal>
      </div>
    </section>
  );
}
