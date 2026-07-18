import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/sections/PageHeader";

export default function CheckoutLoading() {
  return (
    <>
      <PageHeader eyebrow="Checkout" title="Complete your order." />
      <section aria-label="Loading checkout" className="pb-24 md:pb-32 bg-ivory">
        <div className="container grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-start">
          <div className="lg:col-span-8 space-y-10">
            <Skeleton className="h-7 w-56" />
            <div className="space-y-6">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-11 w-full" />
              ))}
            </div>
            <Skeleton className="h-7 w-52 mt-14" />
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-24 w-full" />
              ))}
            </div>
          </div>
          <div className="lg:col-span-4 border border-ink/10 bg-stone-50 p-8 md:p-10 space-y-4">
            <Skeleton className="h-4 w-32" />
            <div className="space-y-3 pt-4">
              <Skeleton className="h-16 w-full" />
              <Skeleton className="h-16 w-full" />
            </div>
            <Skeleton className="h-6 w-32 mt-8" />
          </div>
        </div>
      </section>
    </>
  );
}
