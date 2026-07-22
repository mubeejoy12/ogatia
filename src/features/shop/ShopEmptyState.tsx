import { Button } from "@/components/ui/button";

export type ShopEmptyVariant = "no-results" | "empty-catalogue";

/**
 * Rendered when the current page of products is empty.
 *
 * Two variants, both in the luxury voice:
 *
 * · `no-results` — the customer's filters returned nothing. The primary
 *   action is "Clear all filters"; the secondary is "Speak With Us".
 *
 * · `empty-catalogue` — the atelier hasn't published anything yet (or
 *   the entire published set is unavailable). The primary action is to
 *   speak with the atelier; there is no clear-filters button because
 *   there are no filters set.
 */
export function ShopEmptyState({
  variant = "no-results",
  onClear,
}: {
  variant?: ShopEmptyVariant;
  onClear?: () => void;
}) {
  const isEmptyCatalogue = variant === "empty-catalogue";

  return (
    <div
      role="status"
      aria-live="polite"
      className="mt-16 border-t border-ink/10 pt-24 md:pt-32 pb-8 text-center"
    >
      <p className="eyebrow">
        <span className="rule inline-block align-middle mr-3" />
        {isEmptyCatalogue ? "The atelier" : "Nothing to show"}
        <span className="rule inline-block align-middle ml-3" />
      </p>
      <h3 className="mt-6 font-display text-3xl md:text-4xl tracking-tightest leading-[1.05] max-w-lg mx-auto">
        {isEmptyCatalogue
          ? "New pieces are on the cutting table."
          : "No pieces match your selection."}
      </h3>
      <p className="mt-6 text-stone-700 leading-relaxed max-w-md mx-auto">
        {isEmptyCatalogue
          ? "Our next commission is being finished by hand. Write to the atelier for a preview, or to commission bespoke."
          : "Adjust the filters — or write to the atelier and we will make what you are looking for."}
      </p>
      <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
        {!isEmptyCatalogue && onClear && (
          <Button variant="primary" size="lg" onClick={onClear}>
            Clear all filters
          </Button>
        )}
        <Button
          asChild
          variant={isEmptyCatalogue ? "primary" : "secondary"}
          size="lg"
        >
          <a href="/contact">Speak With Us</a>
        </Button>
      </div>
    </div>
  );
}
