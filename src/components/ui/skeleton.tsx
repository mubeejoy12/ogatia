import { cn } from "@/lib/utils";

/**
 * Reusable skeleton block.
 *
 * Subtle stone-toned pulse — no shimmer, no bounce. Matches the
 * editorial motion vocabulary (respects prefers-reduced-motion via
 * globals.css). Consumers set width/height/aspect via className.
 */
export function Skeleton({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden
      className={cn("animate-pulse bg-stone-200/70", className)}
      {...props}
    />
  );
}
