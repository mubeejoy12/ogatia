# Changelog

All notable changes to Eazi Cut follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Dates use ISO 8601. Unreleased sits at the top; each release moves it below.

---

## [Unreleased]

### Planned
- Deploy v0.8.0 to Vercel production (frontend) alongside a hosted backend instance
- Wire `services/contact.ts` to Resend (real inbox); read `?collection=` query on Contact form to pre-fill enquiry
- Replace top 3 placeholder images with commissioned photography
- NDPR consent banner + `/privacy` + `/terms` pages
- Plausible analytics
- Backend Category / Collection / Order / Auth modules

---

## [0.8.0] — 2026-07-23 — Ticket F003 · Frontend Product API integration

The Shop and Product Detail pages now consume the Spring Boot Product API
end-to-end. No mock product data is referenced by the shop flow.

### Added
- **Typed API client** — `src/lib/api/{client,config,errors,products}.ts` and
  `src/types/api/{envelope,product}.ts`. Envelope-aware `apiGet<T>` with query
  composition, Next.js cache directives, and typed error subclasses
  (`ApiClientError`, `ApiNotFoundError`, `ApiValidationError`, `ApiAuthError`,
  `ApiNetworkError`). All backend contracts mirror
  `com.eazicut.api.products.dto.ProductResponse` verbatim.
- **Product adapter layer** — `src/lib/api/adapters/product.ts`. Handles every
  known contract mismatch: images[] → primary image, shortDescription for
  cards, category.name / collection.slug extraction, computed
  `available = status==="ACTIVE" && stockQuantity>0`, `newArrival` → "New"
  badge. Fields the backend doesn't yet emit (construction, care) become
  empty strings so the UI can hide them.
- **Server-side Shop** — `/shop` is now an async server component with
  `export const dynamic = "force-dynamic"`. Reads URL as single source of
  truth, calls `fetchProducts` with translated backend filter, passes
  `ProductPage` to the client `ShopBrowser`. JSON-LD `ItemList` reflects the
  current page.
- **Price + availability filters, debounced search, backend pagination** —
  `?minPrice`, `?maxPrice`, `?available=true`, `?q=` (300ms debounce with URL
  resync), `?page=` (1-indexed for humans, translated to Spring's 0-indexed
  boundary), `?sort=` (`price-asc`, `price-desc`, `featured` collapses to
  backend default). All arithmetic proven live against a 30-product seed.
- **Empty states** — `ShopEmptyState` picks between `"no-results"`
  (filters returned 0, offers Clear-all) and `"empty-catalogue"` (atelier
  hasn't published, only Speak-With-Us CTA) based on
  `hasActiveFilters(state) + items.length === 0`.
- **Server-side PDP** — `/shop/[slug]` is a full async server component.
  `fetchProductBySlug` catches `ApiNotFoundError` → `notFound()`. Related
  products via `fetchProducts({collection, size: 6})`, filter self, slice 3;
  failure is non-fatal (strip omits). MetaRow hides on empty. `Product`
  JSON-LD reads product id as `sku`, `fullDescription` for description.
- **Branded not-found for PDP** — `src/app/shop/[slug]/not-found.tsx` with
  "This piece is no longer here." copy, Return-to-Shop + Write-to-the-Atelier
  CTAs, `robots: { index: false, follow: false }` so bad slugs don't pollute
  SEO.
- **Dynamic sitemap with graceful degradation** — `src/app/sitemap.ts` pages
  through the backend up to 20 pages × 100 items (2000-product cap). If the
  backend is unreachable at build, logs a warning and returns the static
  routes only (build still succeeds).

### Changed
- **Product type** — added optional `id`, `fullDescription`,
  `shortDescription`, `fabricType`, `color`. Mock products don't carry these;
  API-adapted products do.
- **`getDeliveryEstimate`** extracted from mock into `src/lib/delivery.ts`.
- **`tailwind.config.ts`** — replaced `require("tailwindcss-animate")` with a
  proper ES import; `require` was crashing `next dev` on Node 23.
- **`.env.example`** — `NEXT_PUBLIC_API_URL` block documenting local /
  preview / production URLs.

### Verified (end-to-end)
Live gauntlet against the running Spring Boot backend with 26 seeded products
spanning six colour/fabric families, prices ₦100k–₦800k, one out-of-stock
(`ivory-robe-1`, stock=0), newArrival + featured flags mixed:

| # | Check | Result |
|---|---|---|
| 1  | Real products in Shop (SSR HTML) | ✅ 24 cards on page 1, 2 on page 2 |
| 2  | No mock product imports in shop/PDP/sitemap | ✅ `grep -rn "@/lib/data/products" src/` → 0 matches |
| 3  | Search `?q=onyx` | ✅ narrows to 6, other families excluded |
| 4  | Category filter | ⚠ N/A — backend has no Category REST endpoints yet; frontend axis wired and ready |
| 5  | Collection filter | ⚠ N/A — backend has no Collection REST endpoints yet; frontend axis wired and ready |
| 6  | Filter `?minPrice=400000` | ✅ 19 results |
| 6  | Filter `?maxPrice=200000` | ✅ 4 results |
| 6  | Filter `?minPrice=200000&maxPrice=400000` | ✅ 6 results |
| 7  | Filter `?available=true` | ✅ ivory-robe-1 (stock=0) hidden |
| 8  | Sort `?sort=price-asc` first / `?sort=price-desc` first | ✅ ash-coat-1 @ ₦100k / slate-10 @ ₦800k |
| 9  | Pagination page 1 / page 2 | ✅ disjoint, "of 2" control renders |
| 10 | Shop product links resolve | ✅ every SSR `href="/shop/<slug>"` returns HTTP 200 |
| 11 | PDP loads real backend data | ✅ real name / fabric / colour surfaced from adapter |
| 12 | PDP displays correct info | ✅ title, fabric row, JSON-LD Product + sku, "New" badge from `newArrival=true` |
| 13 | Invalid slug → branded not-found | ✅ "This piece is no longer here" + both CTAs + `noindex` |
| 14 | Backend killed → `/shop` and PDP | ✅ HTTP 200, layout markers intact, zero backend text leaked |
| 15 | Loading states | ✅ routes correctly `ƒ Dynamic`, `Cache-Control: no-store` observed |
| 16 | Empty catalogue (0 products, no filters) | ✅ "cutting table" copy renders, Clear-all correctly absent, Speak-With-Us CTA present |
| 17 | Empty search/filter results | ✅ "match your selection" + Clear-all button |
| 18 | Related products | ✅ fail-safe path proven — PDP renders when related list is empty (products lack collections; adapter emits `[]`, strip omits) |
| 19 | Sitemap with backend up | ✅ 26 `<loc>shop/<slug></loc>` entries emitted |
| 20 | Sitemap with backend down | ✅ HTTP 200, 0 product URLs, 12 static URLs retained |
| 21 | No raw backend leaks anywhere | ✅ strict-regex match count 0 across 4 URLs incl. backend-down (searched: `org.springframework`, `com.eazicut`, `ECONNREFUSED`, `SocketException`, `java.lang`, `ApiNetworkError`) |

### Build metrics (v0.8.0)

| Route | Type | Size | First Load JS |
|---|---|---|---|
| `/shop` | ƒ Dynamic | 8.84 kB | 163 kB |
| `/shop/[slug]` | ƒ Dynamic | 3.37 kB | 157 kB |
| `/sitemap.xml` | ƒ Dynamic | 140 B | 102 kB |
| Total routes | 22 | — | — |
| Typecheck | ✅ clean | — | — |
| Lint | ✅ zero warnings | — | — |
| Production build | ✅ compiles in 16.7s | — | — |

### Known limitations & technical debt (unblocked by F003)
- **Backend Category / Collection REST endpoints are absent** — the
  frontend axes for `?category=` and `?collection=` are wired end-to-end
  and translate to the correct backend filter, but no positive live test
  is possible until those endpoints ship. Same story for related-products
  on the PDP: the adapter+strip renders empty (fail-safe), because no
  seeded product carries a collection. Backend follow-up ticket needed.
- **`ProductResponse` lacks `construction` and `care`** — the adapter
  emits `""` for both; the PDP MetaRow hides on empty. A backend
  enhancement (or a separate PDP-metadata endpoint) is required.
- **`src/lib/data/products.ts` is now unreferenced** — grep confirms
  zero imports anywhere in `src/`. Safe to delete in a separate cleanup
  commit; left in place for now to keep this diff docs-only.
- **No frontend test suite yet** — the shop flow is covered by the live
  E2E gauntlet plus 19/19 backend unit + repository tests; a
  Vitest/Playwright surface for the client is separate scope.

---

## [0.7.0] — 2026-07-10 — Ticket 002 · Collections shopping experience

### Added
- **`/collections/[slug]` dynamic route** — one canonical URL per collection, prerendered as SSG via `generateStaticParams`. 6 detail pages: The Onyx Bespoke, Ivory Wedding, Lagos Heritage, The Essentials, The Noir Tuxedo, Diaspora.
- **Per-detail-page metadata** — title, description, canonical URL, OpenGraph article + Twitter summary cards with collection hero image.
- **`getCollection(slug)` / `getCollectionSlugs()` / `getRelatedCollections(slug)`** accessors in `src/lib/data/collections.ts`. `getCollection` calls `notFound()` on unknown slug → hits branded 404. Same API shape a future `fetch('/api/collections/{slug}')` will have — zero component churn when backend lands.
- **Enriched `Collection` schema** — `tagline`, `story[]` paragraphs, `signaturePieces[]`, `fabric`, optional `startingPrice`. All fields real, all consumed on the detail page.
- **`CollectionPage` JSON-LD** on `/collections` — indexes each collection as a `CreativeWork` for Google's category hub understanding.
- **Sitemap** now includes 6 collection detail routes at priority 0.8 with weekly `changeFrequency` on index / monthly on details.
- **"Related collections" strip** at the foot of each detail page — deterministic rotation, reuses `CollectionCard`.
- **"Shop the Collection" CTA** on every detail page pointing at `/shop?collection={slug}` — the URL Ticket 003 will consume. Ready today.
- **"Commission Bespoke" CTA** pointing at `/contact?collection={slug}` — Contact form (Resend integration next) can read this to pre-fill enquiries.

### Changed
- **`/collections` rebuilt** from a single-page 6-editorial-spread layout into a proper 3-column responsive index grid (1/2/3 columns on mobile/tablet/desktop). More scalable, faster to scan, matches luxury-house convention (Zegna, Brioni).
- **`CollectionCard` enhanced** per Ticket 002 brief — added dynamic pieces count + explicit "Explore the Collection" text CTA + focus-visible gold ring for keyboard nav + `sizes` attribute tuned for the new 3-column grid.
- **`CollectionCard` default `href`** changed from `/collections#{slug}` (hash anchor into the old single-page layout) to `/collections/{slug}` (canonical detail URL). Home's Featured Collections and every consumer updated transparently — the CTA now leads to a real page.
- **Sitemap builder** refactored to compose static routes + dynamic collection routes from the same data source.

### Build metrics (v0.7.0)
- Static routes: **19/19** prerendered (up from 13)
- Collections index HTML: 514 B / 147 kB first-load JS
- Each collection detail: 514 B / 147 kB first-load JS
- Typecheck: ✅ pass
- Lint: ✅ zero warnings
- Production build: ✅ pass

---

## [0.6.0] — 2026-07-10 — Homepage completed to production quality

### Added
- **Dynamic favicon** (`src/app/icon.tsx`) — "EC" monogram in ink with gold underscore, generated by `ImageResponse`. No static asset required.
- **Apple touch icon** (`src/app/apple-icon.tsx`) at 180×180 for iOS home-screen bookmarking.
- **Web app manifest** (`src/app/manifest.ts`) — theme colours, standalone display mode, install-to-home-screen name.
- **Branded 404 page** (`src/app/not-found.tsx`) — matches luxury voice; "The page you were looking for no longer resides here." Return-home + Speak-with-us CTAs. `robots: noindex, nofollow`.
- **Branded root error boundary** (`src/app/error.tsx`) — "Something went briefly amiss." Try-again + Return-home CTAs. Shows error `digest` reference for support.
- **`viewport` export in `layout.tsx`** — theme colour tokens for both light/dark browser chrome; disables auto-detection of phone/email/address (avoids iOS blue-link mangling of prices and addresses).
- **`WebSite` JSON-LD schema** on the homepage alongside `Organization` + `LocalBusiness` — makes Eazi Cut eligible for Google sitelinks search box.

### Changed
- **Hero image** now includes `fetchPriority="high"`, `quality={90}`, and `object-[center_35%]` — improves LCP and holds the model's face in the visible crop across all viewport heights.
- **All homepage sections** now carry explicit `aria-label` attributes for screen-reader landmark navigation (Introduction, Featured collections, Why choose Eazi Cut, The tailoring process, Client voices, and dynamic CTA labels).

### Build metrics (v0.6.0)
- Static routes: 13/13 prerendered
- Home HTML: **3.24 kB**
- Home first-load JS: **157 kB**
- Typecheck: ✅ pass
- Lint: ✅ zero warnings
- Production build: ✅ pass

---

## [0.5.0] — 2026-07-10 — Homepage hardened for launch

### Added
- `BrandStatement` editorial section between Hero and Featured Collections — provides luxury-house tempo before the collection grid.
- Homepage-specific `metadata` export with canonical URL and page-tuned Open Graph.
- JSON-LD `Organization` + `LocalBusiness` structured data on the homepage — improves Google Knowledge Panel and social preview cards.
- ESLint config (`next/core-web-vitals` + `next/typescript`) — `npm run lint` now runs against `src/`.

### Changed
- Testimonials attribution: removed fictional first-name/last-initial names; kept role + city only. Aligns with luxury-house convention (Hermès/Zegna do not name individual clients in marketing copy) and eliminates placeholder attribution.
- Testimonials markup upgraded to proper `<figure>` / `<blockquote>` / `<figcaption>` semantics.

### Fixed
- Testimonials component `key` prop no longer relies on removed `name` field — uses `role-city` composite.

---

## [0.4.0] — 2026-07-10 — Documentation architecture

### Added
- `/MASTER_CONTEXT.md` — one-file project summary for AI cold-starts.
- `/CLAUDE.md` (root) — AI entry point with reading order and non-negotiables.
- `/docs/README.md` — documentation index.
- `/docs/management/PROJECT_STATUS.md` — weekly status tracker.
- `/docs/management/ROADMAP.md` — consolidated now → launch plan.
- `/docs/management/BACKLOG.md` — MoSCoW-prioritised backlog.

### Documented
- Product Discovery & Documentation Refactoring audit (this session): 35 markdown files scored; duplicates, overlaps, and archive candidates identified. No files moved yet — awaiting approval.

---

## [0.3.0] — 2026-07-10 — Asset centralisation

### Added
- `src/lib/assets.ts` — centralised image asset registry. Every image in the app resolves through this file. Placeholder-swap is a one-line change per slot.
- Typed `CollectionSlug` union to prevent slug drift between assets and data.

### Changed
- Every component and data file that referenced an image URL now imports from `src/lib/assets.ts`. Zero hardcoded image URLs remain in the codebase.
- Collection slugs updated to evocative luxury identifiers (`the-onyx-bespoke`, `ivory-wedding`, `lagos-heritage`, `the-essentials`, `the-noir-tuxedo`, `diaspora`).

---

## [0.2.0] — 2026-07-10 — Architecture refinement

### Added
- `src/features/contact/` — feature-module pattern for Contact form.
- `src/hooks/useContactForm.ts` — form submission state hook.
- `src/services/contact.ts` — service layer stub (mock; awaits Resend).
- `src/types/contact.ts` — shared domain types.
- Reusable `CollectionCard` component composed by Home + Collections page.
- shadcn primitives: `Input`, `Textarea`, `Select`, `Label`.

### Fixed
- `package.json` pinned to stable React 19 (was `19.0.0-rc-...` which conflicted with framer-motion's peer-dep). `npm install` now succeeds without `--legacy-peer-deps`.

### Removed
- Leftover `eazi-cut/` create-next-app scaffold that was serving the default "coming soon" page instead of the built site.

---

## [0.1.0] — 2026-07-10 — Marketing site scaffold

### Added
- Next.js 15 App Router + TypeScript (strict) + Tailwind + Framer Motion.
- shadcn/ui Button primitive.
- Five pages: Home, About, Collections, Lookbook, Contact.
- Home sections: Hero, Featured Collections, Why Choose Eazi Cut, Tailoring Process, Testimonials, CTA.
- Layout components: Navbar (sticky, transparent-over-hero, mobile drawer), Footer.
- SEO: root metadata, Open Graph, Twitter cards, `robots.ts`, `sitemap.ts`, skip-to-content link, semantic `<main>`.
- Editorial motion primitives: `Reveal`, `Stagger`, `StaggerItem`.
- Design tokens in `tailwind.config.ts`: ink `#0A0A0A` / ivory `#F5F1EA` / gold `#B8893E` / soft `#D4A85C` / deep `#8C6628`; Playfair Display + Inter via `next/font`.
- Editorial cubic-bezier easing curve `cubic-bezier(0.22, 1, 0.36, 1)`.

---

## Versioning

- **v0.x** — pre-launch iterations (current)
- **v1.0.0** — initial production release: marketing site live on `eazicut.com`, Contact wired, real photography, NDPR-compliant.
- **v2.x** — commerce phase (catalogue, cart, checkout, Paystack, accounts).
- **v3.x** — bespoke phase (measurements, commissioning, atelier ops).
