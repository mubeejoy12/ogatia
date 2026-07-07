import { cn } from "@/lib/utils";
import { Reveal } from "@/components/motion/Reveal";

export function SectionHeader({
  eyebrow,
  title,
  description,
  align = "left",
  className,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  align?: "left" | "center";
  className?: string;
}) {
  return (
    <Reveal
      className={cn(
        "max-w-3xl",
        align === "center" && "mx-auto text-center",
        className
      )}
    >
      {eyebrow && (
        <p className="eyebrow">
          <span className="rule inline-block align-middle mr-3" />
          {eyebrow}
        </p>
      )}
      <h2 className="mt-5 font-display tracking-tightest text-4xl md:text-5xl lg:text-6xl leading-[1.05]">
        {title}
      </h2>
      {description && (
        <p className="mt-6 text-stone-700 text-lg leading-relaxed max-w-2xl">
          {description}
        </p>
      )}
    </Reveal>
  );
}
