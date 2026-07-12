import { Button } from "@/components/ui/button";

/**
 * Rendered when the current filter set produces no products.
 *
 * Understated, in-brand voice — luxury shoppers respond to considered
 * pauses, not "No results!" alerts. Offers a single, quiet action
 * (clear all filters) plus a subtle route to the atelier for anything
 * that isn't in the current catalogue.
 */
export function ShopEmptyState({ onClear }: { onClear: () => void }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="mt-16 border-t border-ink/10 pt-24 md:pt-32 pb-8 text-center"
    >
      <p className="eyebrow">
        <span className="rule inline-block align-middle mr-3" />
        Nothing to show
        <span className="rule inline-block align-middle ml-3" />
      </p>
      <h3 className="mt-6 font-display text-3xl md:text-4xl tracking-tightest leading-[1.05] max-w-lg mx-auto">
        No pieces match your selection.
      </h3>
      <p className="mt-6 text-stone-700 leading-relaxed max-w-md mx-auto">
        Adjust the filters — or write to the atelier and we will make what you
        are looking for.
      </p>
      <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
        <Button variant="primary" size="lg" onClick={onClear}>
          Clear all filters
        </Button>
        <Button asChild variant="secondary" size="lg">
          <a href="/contact">Speak With Us</a>
        </Button>
      </div>
    </div>
  );
}
