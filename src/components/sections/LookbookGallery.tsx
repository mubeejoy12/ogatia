import Image from "next/image";
import { lookbook } from "@/lib/data/lookbook";
import { cn } from "@/lib/utils";
import { Stagger, StaggerItem } from "@/components/motion/Reveal";

const spanClass = {
  tall: "md:col-span-5 md:row-span-2 aspect-[3/4]",
  wide: "md:col-span-7 aspect-[4/3]",
  square: "md:col-span-5 aspect-square",
} as const;

export function LookbookGallery() {
  return (
    <section className="pb-24 md:pb-32 bg-ivory">
      <div className="container">
        <Stagger className="grid grid-cols-1 md:grid-cols-12 gap-6 md:gap-8 auto-rows-auto">
          {lookbook.map((img) => (
            <StaggerItem
              key={img.src}
              className={cn("group relative overflow-hidden", spanClass[img.span])}
            >
              <Image
                src={img.src}
                alt={img.alt}
                fill
                sizes="(min-width: 768px) 50vw, 100vw"
                className="object-cover transition-transform duration-[1400ms] ease-editorial group-hover:scale-[1.04]"
              />
              <div className="absolute inset-x-0 bottom-0 p-6 bg-gradient-to-t from-ink/70 to-transparent text-ivory opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                <p className="eyebrow text-ivory/70">{img.collection}</p>
                <p className="mt-2 font-display text-xl tracking-tightest">
                  {img.caption}
                </p>
              </div>
            </StaggerItem>
          ))}
        </Stagger>
      </div>
    </section>
  );
}
