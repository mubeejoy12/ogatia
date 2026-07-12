"use client";

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
    <div className="mt-10 border-y border-ink/10 py-6">
      <div className="flex flex-col lg:flex-row lg:items-end gap-6 lg:gap-8">
        {/* Search */}
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
              value={state.q}
              onChange={(e) => patch({ q: e.target.value })}
              className="pl-6"
            />
          </div>
        </div>

        {/* Category */}
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

        {/* Collection */}
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

        {/* Sort */}
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
    </div>
  );
}
