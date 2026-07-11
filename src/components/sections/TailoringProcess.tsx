import Image from "next/image";
import { tailoringProcess } from "@/lib/data/process";
import { SectionHeader } from "./SectionHeader";
import { Reveal, Stagger, StaggerItem } from "@/components/motion/Reveal";
import { assets } from "@/lib/assets";

export function TailoringProcess() {
  return (
    <section
      aria-label="The tailoring process"
      className="py-24 md:py-32 bg-stone-50"
    >
      <div className="container">
        <SectionHeader
          eyebrow="The Process"
          title="From consultation to delivery — five quiet steps."
          description="Bespoke tailoring is a conversation. Ours has a rhythm, refined over thousands of commissions."
        />

        <div className="mt-20 grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
          <Reveal className="lg:col-span-5 lg:sticky lg:top-28">
            <div className="relative aspect-[3/4] overflow-hidden">
              <Image
                src={assets.process.workshop.src}
                alt={assets.process.workshop.alt}
                fill
                sizes="(min-width: 1024px) 40vw, 100vw"
                className="object-cover"
              />
            </div>
          </Reveal>

          <Stagger className="lg:col-span-7 divide-y divide-ink/10">
            {tailoringProcess.map((s) => (
              <StaggerItem key={s.index} className="py-10 first:pt-0">
                <div className="grid grid-cols-12 gap-6">
                  <p className="col-span-2 font-display text-gold text-2xl">
                    {s.index}
                  </p>
                  <div className="col-span-10">
                    <h3 className="font-display text-3xl tracking-tightest">
                      {s.title}
                    </h3>
                    <p className="mt-3 text-stone-700 leading-relaxed max-w-lg">
                      {s.body}
                    </p>
                  </div>
                </div>
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </div>
    </section>
  );
}
