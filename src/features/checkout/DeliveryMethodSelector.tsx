"use client";

import { cn } from "@/lib/utils";
import { formatNaira } from "@/lib/format";
import {
  deliveryMethods,
  type DeliveryMethodId,
} from "./deliveryMethods";

/**
 * Delivery method chooser.
 *
 * Rendered as a stack of labelled radio cards — the pattern luxury
 * checkouts use to expose price, timeline and description at the same
 * moment as the choice, so customers don't have to click into a modal
 * to compare. Semantic <label> + radio for free keyboard + AT support.
 */
export function DeliveryMethodSelector({
  selected,
  onSelect,
}: {
  selected: DeliveryMethodId | null;
  onSelect: (id: DeliveryMethodId) => void;
}) {
  return (
    <fieldset>
      <legend className="sr-only">Delivery method</legend>
      <div className="space-y-3">
        {deliveryMethods.map((method) => {
          const isActive = selected === method.id;
          return (
            <label
              key={method.id}
              className={cn(
                "flex gap-4 border p-5 cursor-pointer transition-colors duration-300",
                isActive
                  ? "border-ink bg-stone-50"
                  : "border-ink/15 hover:border-ink/40"
              )}
            >
              <input
                type="radio"
                name="delivery-method"
                value={method.id}
                checked={isActive}
                onChange={() => onSelect(method.id)}
                className="mt-1 accent-ink"
              />
              <div className="flex-1 min-w-0">
                <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1 justify-between">
                  <p className="font-display text-lg md:text-xl tracking-tightest">
                    {method.name}
                  </p>
                  <p className="font-display text-lg tracking-tight text-ink">
                    {method.price === 0 ? "Complimentary" : formatNaira(method.price)}
                  </p>
                </div>
                <p className="mt-1 text-xs uppercase tracking-widest text-stone-500">
                  {method.eta}
                </p>
                <p className="mt-3 text-sm text-stone-700 leading-relaxed">
                  {method.description}
                </p>
              </div>
            </label>
          );
        })}
      </div>
    </fieldset>
  );
}
