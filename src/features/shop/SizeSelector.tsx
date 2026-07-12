"use client";

import { cn } from "@/lib/utils";

/**
 * A chip row of available sizes. One-at-a-time selection.
 *
 * Rendered as a native radiogroup so keyboard, mobile pickers and screen
 * readers all work without extra ARIA plumbing. Selected chip inverts to
 * ink/ivory; unselected chips carry a hairline border and hover state.
 */
export function SizeSelector({
  sizes,
  selected,
  onSelect,
  id = "size-selector",
}: {
  sizes: readonly string[];
  selected: string | null;
  onSelect: (size: string) => void;
  id?: string;
}) {
  return (
    <div role="radiogroup" aria-labelledby={`${id}-label`}>
      <p
        id={`${id}-label`}
        className="eyebrow text-stone-500 flex items-baseline justify-between"
      >
        <span>Choose your size</span>
        {selected && (
          <span className="text-ink normal-case tracking-normal text-xs font-sans">
            Selected · {selected}
          </span>
        )}
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {sizes.map((size) => {
          const isSelected = selected === size;
          return (
            <button
              key={size}
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => onSelect(size)}
              className={cn(
                "min-w-14 px-4 py-3 border text-sm tracking-wide transition-colors duration-300",
                "focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-ivory",
                isSelected
                  ? "bg-ink text-ivory border-ink"
                  : "bg-transparent text-ink border-ink/30 hover:border-ink"
              )}
            >
              {size}
            </button>
          );
        })}
      </div>
    </div>
  );
}
