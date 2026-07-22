"use client";

import { useEffect, useRef, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { productCategories, type ProductCategory } from "@/types/product";
import { collectionSlugs, type CollectionSlug } from "@/lib/assets";
import { collections } from "@/lib/data/collections";
import {
  sortLabels,
  sortModes,
  type FilterState,
  type SortMode,
} from "./filters";

/**
 * Toolbar rendered above the product grid.
 *
 * Native `<select>` primitives — chosen over custom dropdowns because they
 * carry keyboard, screen-reader and mobile-picker behaviour for free.
 * Styled sparingly (underline + eyebrow label) to keep the luxury tone.
 *
 * The parent owns the filter state; this component is purely presentational
 * and reports every change up through `onChange`.
 *
 * <p><strong>Debounced search.</strong> The search input carries its own
 * local state; commits to the URL 300ms after the last keystroke so
 * typing "wool" doesn't fire four backend requests. Dropdowns and the
 * availability toggle commit immediately (discrete actions). Price
 * inputs commit on blur / Enter.
 */
export function ShopToolbar({
  state,
  onChange,
}: {
  state: FilterState;
  onChange: (next: FilterState) => void;
}) {
  const patch = (partial: Partial<FilterState>) =>
    onChange({ ...state, ...partial });

  return (
    <div className="mt-10 border-y border-ink/10 py-6 space-y-6">
      {/* Row 1 — search, category, collection, sort */}
      <div className="flex flex-col lg:flex-row lg:items-end gap-6 lg:gap-8">
        <DebouncedSearch
          value={state.q}
          onCommit={(q) => patch({ q })}
        />

        <div className="lg:w-52">
          <Label htmlFor="shop-category" className="mb-3">
            Category
          </Label>
          <Select
            id="shop-category"
            value={state.category ?? ""}
            onChange={(e) =>
              patch({
                category: (e.target.value as ProductCategory) || null,
              })
            }
          >
            <option value="">All categories</option>
            {productCategories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
        </div>

        <div className="lg:w-56">
          <Label htmlFor="shop-collection" className="mb-3">
            Collection
          </Label>
          <Select
            id="shop-collection"
            value={state.collection ?? ""}
            onChange={(e) =>
              patch({
                collection: (e.target.value as CollectionSlug) || null,
              })
            }
          >
            <option value="">All collections</option>
            {collectionSlugs.map((slug) => (
              <option key={slug} value={slug}>
                {collections.find((c) => c.slug === slug)?.name ?? slug}
              </option>
            ))}
          </Select>
        </div>

        <div className="lg:w-52">
          <Label htmlFor="shop-sort" className="mb-3">
            Sort by
          </Label>
          <Select
            id="shop-sort"
            value={state.sort}
            onChange={(e) => patch({ sort: e.target.value as SortMode })}
          >
            {sortModes.map((mode) => (
              <option key={mode} value={mode}>
                {sortLabels[mode]}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {/* Row 2 — price range + availability */}
      <div className="flex flex-col lg:flex-row lg:items-end gap-6 lg:gap-8">
        <PriceInput
          id="shop-min-price"
          label="Min price (₦)"
          value={state.minPrice}
          onCommit={(minPrice) => patch({ minPrice })}
        />
        <PriceInput
          id="shop-max-price"
          label="Max price (₦)"
          value={state.maxPrice}
          onCommit={(maxPrice) => patch({ maxPrice })}
        />

        <div className="flex items-center gap-3 lg:pb-3">
          <input
            id="shop-available"
            type="checkbox"
            checked={state.available === true}
            onChange={(e) =>
              patch({ available: e.target.checked ? true : null })
            }
            className="w-4 h-4 accent-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2"
          />
          <Label htmlFor="shop-available" className="mb-0 cursor-pointer">
            In stock only
          </Label>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Debounced search input
// ---------------------------------------------------------------------------

/**
 * Locally-held search value that syncs to the URL 300ms after the last
 * keystroke. Also resyncs downward when the external `value` changes
 * (e.g. Clear-all-filters wipes it back to empty).
 */
function DebouncedSearch({
  value,
  onCommit,
}: {
  value: string;
  onCommit: (next: string) => void;
}) {
  const [local, setLocal] = useState(value);
  const lastCommitted = useRef(value);

  // External → local when the URL param changes for reasons other than
  // this input's typing (clear-all button, back navigation, etc.).
  useEffect(() => {
    if (value !== lastCommitted.current) {
      setLocal(value);
      lastCommitted.current = value;
    }
  }, [value]);

  // Local → external, debounced.
  useEffect(() => {
    if (local === lastCommitted.current) return;
    const t = setTimeout(() => {
      lastCommitted.current = local;
      onCommit(local);
    }, 300);
    return () => clearTimeout(t);
  }, [local, onCommit]);

  return (
    <div className="lg:flex-1 lg:max-w-md">
      <Label htmlFor="shop-search" className="mb-3">
        Search
      </Label>
      <div className="relative">
        <Search
          size={16}
          className="pointer-events-none absolute left-0 top-1/2 -translate-y-1/2 text-stone-500"
          aria-hidden
        />
        <Input
          id="shop-search"
          type="search"
          placeholder="Search pieces, fabric, occasion…"
          value={local}
          onChange={(e) => setLocal(e.target.value)}
          className="pl-6"
        />
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Commit-on-blur price input
// ---------------------------------------------------------------------------

/**
 * Number input that stores its own text state and commits on blur or
 * Enter — avoids a URL change on every keystroke while still feeling
 * responsive.
 */
function PriceInput({
  id,
  label,
  value,
  onCommit,
}: {
  id: string;
  label: string;
  value: number | null;
  onCommit: (next: number | null) => void;
}) {
  const [local, setLocal] = useState(value === null ? "" : String(value));

  useEffect(() => {
    setLocal(value === null ? "" : String(value));
  }, [value]);

  const commit = () => {
    const trimmed = local.trim();
    if (trimmed === "") {
      if (value !== null) onCommit(null);
      return;
    }
    const n = Number(trimmed);
    if (!Number.isFinite(n) || n < 0) {
      // Reset to committed value.
      setLocal(value === null ? "" : String(value));
      return;
    }
    const rounded = Math.floor(n);
    if (rounded !== value) onCommit(rounded);
  };

  return (
    <div className="lg:w-40">
      <Label htmlFor={id} className="mb-3">
        {label}
      </Label>
      <Input
        id={id}
        type="number"
        inputMode="numeric"
        min={0}
        step={1000}
        placeholder="—"
        value={local}
        onChange={(e) => setLocal(e.target.value)}
        onBlur={commit}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            commit();
          }
        }}
      />
    </div>
  );
}
