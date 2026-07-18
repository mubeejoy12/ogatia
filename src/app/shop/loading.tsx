import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/sections/PageHeader";

/**
 * Route-level loading UI for /shop. Rendered by Next during route
 * transitions and before the streamed page is ready. Preserves the
 * grid rhythm so there is no visible layout jump when the real content
 * arrives.
 */
export default function ShopLoading() {
  return (
    <>
      <PageHeader
        eyebrow="Shop"
        title="The wardrobe, ready to wear."
        intro="Every piece is cut in the Lagos atelier."
      />
      <section aria-label="Loading product catalogue" className="pb-24 md:pb-32 bg-ivory">
        <div className="container">
          <Skeleton className="h-4 w-40 mt-1" />
          <div className="mt-10 border-y border-ink/10 h-24" />
          <div className="mt-14 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-16">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i}>
                <Skeleton className="aspect-[4/5] w-full" />
                <div className="mt-5 space-y-3">
                  <Skeleton className="h-3 w-20" />
                  <Skeleton className="h-6 w-3/4" />
                  <Skeleton className="h-4 w-24" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
