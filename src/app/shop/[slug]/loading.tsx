import { Skeleton } from "@/components/ui/skeleton";

/**
 * PDP loading UI. Matches the two-column layout so the reveal is
 * a content swap, not a jump.
 */
export default function ProductLoading() {
  return (
    <>
      <div className="bg-ivory border-b border-ink/10 h-12" />
      <section aria-label="Loading product" className="pt-14 md:pt-20 pb-24 md:pb-32 bg-ivory">
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-start">
          <div className="lg:col-span-7">
            <Skeleton className="aspect-[4/5] w-full" />
          </div>
          <div className="lg:col-span-5 lg:pl-4 space-y-6">
            <Skeleton className="h-3 w-40" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-24 w-full" />
            <div className="space-y-3 pt-6">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-5/6" />
              <Skeleton className="h-4 w-4/6" />
            </div>
            <div className="pt-6 flex gap-2">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-11 w-14" />
              ))}
            </div>
            <div className="pt-4 flex gap-3">
              <Skeleton className="h-14 flex-1" />
              <Skeleton className="h-14 w-32" />
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
