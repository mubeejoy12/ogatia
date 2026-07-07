import { Reveal } from "@/components/motion/Reveal";

export function PageHeader({
  eyebrow,
  title,
  intro,
}: {
  eyebrow: string;
  title: string;
  intro?: string;
}) {
  return (
    <section className="pt-40 pb-16 md:pt-48 md:pb-24 bg-ivory">
      <div className="container">
        <Reveal className="max-w-4xl">
          <p className="eyebrow">
            <span className="rule inline-block align-middle mr-3" />
            {eyebrow}
          </p>
          <h1 className="mt-6 font-display text-5xl md:text-7xl lg:text-8xl leading-[0.98] tracking-tightest">
            {title}
          </h1>
          {intro && (
            <p className="mt-8 max-w-2xl text-lg text-stone-700 leading-relaxed">
              {intro}
            </p>
          )}
        </Reveal>
      </div>
    </section>
  );
}
