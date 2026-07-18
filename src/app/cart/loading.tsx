import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/sections/PageHeader";

export default function CartLoading() {
  return (
    <>
      <PageHeader eyebrow="The Bag" title="Your selection." />
      <section aria-label="Loading bag" className="pb-24 md:pb-32 bg-ivory">
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-start">
          <div className="lg:col-span-8 space-y-6">
            <Skeleton className="h-4 w-32" />
            {Array.from({ length: 2 }).map((_, i) => (
              <div key={i} className="grid grid-cols-12 gap-4 py-6 border-b border-ink/10">
                <Skeleton className="col-span-4 sm:col-span-3 lg:col-span-2 aspect-[4/5]" />
                <div className="col-span-8 sm:col-span-9 lg:col-span-10 space-y-3">
                  <Skeleton className="h-3 w-16" />
                  <Skeleton className="h-6 w-2/3" />
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-9 w-40" />
                </div>
              </div>
            ))}
          </div>
          <div className="lg:col-span-4 border border-ink/10 bg-stone-50 p-8 md:p-10 space-y-4">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-4 w-16" />
            <Skeleton className="h-12 w-full mt-6" />
          </div>
        </div>
      </section>
    </>
  );
}
