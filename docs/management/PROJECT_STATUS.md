# Project Status

**Reporting date:** 2026-07-31
**Reporting owner:** Beejoy Technologies (delivery)
**Client:** Eazi Cut / Mubarak Abubakar

---

## At a glance

| | Status |
|---|---|
| **Phase** | 1 of 3 — Marketing Website |
| **Overall health** | 🟢 Green |
| **Schedule** | On track for local completion; deployment pending |
| **Scope** | Stable — see BACKLOG for change requests |
| **Budget / effort** | Within envelope |
| **Blockers** | None active |

---

## Phase progress

| Phase | Scope | Status | Target |
|---|---|---|---|
| **1. Marketing Website** | Home, About, Collections, Lookbook, Contact | 🟢 90% — awaiting deployment + real photography | Aug 2026 |
| **2. Product Catalogue & Commerce** | Categories, PDPs, cart, checkout, Paystack | 🟡 Shop + PDP live (F003); Categories + Collections (B003); Authentication (B004); Cart end-to-end with guest→server merge (B005) — Order module + Paystack pending | Q4 2026 |
| **3. Bespoke Tailoring Engine** | Measurements, order lifecycle, atelier ops | ⚪ Not started | Q1 2027 |

---

## What shipped this reporting period

- ✅ **Ticket B005 — Shopping Cart (v0.11.0)** — production-ready cart end-to-end. Guests keep their bag in `localStorage`; authenticated customers get a persistent server-side cart with per-line snapshots, read-time issue reporting, and a one-shot merge from guest to server at login. Six atomic stages: (1) V6 migration + Cart/CartItem entities + repositories + DTOs + `CartMapper` + `CartService.readForUser`; (2) CRUD — add/patch/remove/clear + `CartController` + `EndpointAuthorizationTest` extension; (3) `CartIssueDetector` emitting `price_changed` / `out_of_stock` / `size_unavailable` / `product_removed` on every read; (4) `POST /cart/merge` with silent skip for bad slugs / retired sizes and per-cart line cap enforcement; (5) frontend dual-mode `CartContext` rewrite — guest → server transition on login fires `POST /cart/merge`, `useCart` public API surface preserved so PDP/checkout compile unchanged; (6) full E2E gauntlet + docs (this stage). **CRITICAL invariant baked into entity + migration Javadoc:** cart snapshot price is historical/display only — never silently becomes the charged price. The Order pipeline (B006) enforces "customer must confirm changed price before Order is created" and writes an immutable `OrderItem` price. Backend tests 183/183 PASS (was 129 pre-B005; +54 new). 20/21 live E2E items pass (the one "fail" was Jackson formatting `500000.0` vs `500000.0000` — same value).
- ✅ **Ticket B004 — Authentication & Authorization (v0.10.0)** — production-ready auth end-to-end. Retires the B001 scaffold (`spring.security.user.*` in-memory user, `permitAll` filter chain default, HTTP Basic) and replaces it with: DB-backed users (V4 migration, BCrypt password hashing, case-insensitive email uniqueness via the same `name_lower` pattern V2/V3 use), JWT bearer tokens (15-min HS256 access) + rotating refresh tokens (7-day, V5 migration, hash-only storage, HttpOnly Secure SameSite=Lax cookie scoped to `/api/v1/auth`), per-IP + per-email login rate limiting (5 attempts / 15 min rolling window, clean `LoginRateLimiter` interface with in-memory impl for launch), a secure-by-default `.anyRequest().authenticated()` filter chain with explicit public allowlist, and constant-time login (BCrypt runs even on unknown email to close the timing side-channel). Frontend gets `/login`, `/register`, `/account` (branded to the luxury voice — "Sign in to the Atelier", "Open an account"), an in-memory-access-token `AuthContext` with mount-time silent refresh, and a middleware gate for `/account` powered by a companion browser-visible session-marker cookie. Eight atomic stages: (1) User domain + PasswordEncoder + DB-backed UserDetailsService + dev admin seed via env vars; (2) registration + reusable `@ValidPassword` (8–128 length, blocklist per D6); (3) JWT infrastructure + secure-by-default filter chain flip + `EndpointAuthorizationTest` walking every mapped endpoint; (4) login + rate limiting + HTTP Basic retirement; (5) refresh-token persistence + rotation + refresh/logout endpoints; (6) `/auth/me` + `FullAuthFlowTest` end-to-end MockMvc loop; (7) frontend auth surface + middleware + session-marker cookie; (8) full E2E security gauntlet + docs (this stage). Backend tests 129/129 PASS (was 19 pre-B004; +110 new). 33/33 live E2E items verified positive.
- ✅ **Ticket B003 — Category & Collection Reference Data (v0.9.0)** — production-ready CRUD REST for both taxonomies at `/api/v1/categories` and `/api/v1/collections` (public reads, admin writes), plus a dev-only idempotent seeder that ensures the six categories and six collections the frontend expects are present on every dev boot. Six atomic stages: (1) Category DTOs + mapper + exceptions + service with case-insensitive name normalisation and domain-level in-use check; (2) Category controller + service/repo tests + V2 migration adding `categories.name_lower` unique index (portable across H2 and PostgreSQL); (3) Collection DTOs/mapper/exceptions/service — direct parallel; (4) Collection controller + tests + V3 migration; (5) `ReferenceDataSeeder` `@Profile("dev")` `ApplicationRunner` with per-row `existsBySlug` idempotency guards; (6) frontend hardening + full E2E. Frontend additions: typed `src/lib/api/categories.ts` + `collections.ts` + matching `src/types/api/*.ts` (no redesign per D4); `src/features/shop/filters.ts` now `console.warn`s when the URL carries an unknown taxonomy value; three dead re-exports and one mock import removed from `src/features/shop/backendFilter.ts`. 14 live E2E items verified positive against the seeded taxonomy — category filter, collection filter, combined, related-products PDP (now working because seeded products share collections), reference-endpoint reads, and safe fallback for unknown taxonomy. Backend tests 56/56 PASS (was 19 pre-B003; +37 new). Typecheck clean, lint zero warnings, build 22 routes.
- ✅ **Ticket F003 — Frontend Product API Integration (v0.8.0)** — `/shop` and `/shop/[slug]` no longer read mock product data. Six atomic stages: (1) typed API client foundation — `apiGet<T>`, envelope types, error subclasses, `NEXT_PUBLIC_API_URL` config, product adapter mapping every documented contract mismatch; (2) `/shop` rewired as an async server component reading URL as single source of truth, backend pagination, `mapApiProductPage` adapter; (3) price/availability filters, debounced search (300ms local → URL commit), full sort/pagination axes translated to Spring's `field,direction` syntax; (4) empty-state disambiguation ("no-results" vs "empty-catalogue") + 2-row toolbar skeleton; (5) `/shop/[slug]` full server-component rewrite — `fetchProductBySlug` catches `ApiNotFoundError` → branded `not-found.tsx`, related products via collection query, MetaRow hides on empty, dynamic `sitemap.ts` pages backend up to 2000 products with graceful build-time fallback; (6) end-to-end verification against a 26-product seed — every DoD axis proven live (search narrows, price filters arithmetic, sort ordering, pagination disjoint pages, PDP renders JSON-LD Product with sku + newArrival badge, invalid slug returns branded not-found with `noindex`, backend killed returns 200 with layout intact and zero backend text leaked). Typecheck ✅, lint ✅ zero warnings, production build ✅ (16.7s, 22 routes, `/shop` and `/shop/[slug]` correctly ƒ Dynamic). `grep -rn "@/lib/data/products" src/` returns zero matches.
- ✅ **Ticket B-INFRA-001 — Database Migration Infrastructure** — Flyway is now the schema authority in every profile (dev H2, test H2, prod PostgreSQL). Four stages: (1) `flyway-core` + `flyway-database-postgresql` deps + disabled config block; (2) `V1__initial_schema.sql` baseline derived directly from Hibernate's canonical DDL dump (categories, collections, products with named `uk_product_slug`/`uk_product_sku` + status CHECK constraint, product_images, product_sizes/product_tags with composite PKs) — plus one entity annotation `@JdbcTypeCode(SqlTypes.VARCHAR)` on `Product.status` to force portable VARCHAR enum storage across H2 and PostgreSQL; (3) enabled Flyway + flipped `hibernate.ddl-auto` from `create-drop` to `validate` in dev + patched `@DataJpaTest` to force validate in the slice; (4) documentation. Live-verified end-to-end: boot log shows *"Migrating schema PUBLIC to version 1 - initial schema"*, Hibernate validate succeeds, POST/GET /products round-trip works against the migrated schema. 19/19 tests still pass. Any future entity change now requires a matching Vn migration or the app fails-fast on startup.
- ✅ **Ticket B002 — Product Module (backend)** — full production-ready Product feature on top of the Spring Boot foundation (B001). Six atomic stages: (1) entities + auditing base + Category/Collection minimal entities + soft-delete via `@SQLDelete`/`@SQLRestriction`; (2) repositories + `PagedResponse` envelope + validated DTOs + `ProductFilterCriteria`; (3) service layer + MapStruct mapper + `ConflictException` base + duplicate-slug/sku exceptions with 409 mapping; (4) `/api/v1/products` REST CRUD (GET list / by id / by slug, POST/PUT/DELETE) + method-level `@PreAuthorize("hasRole('ADMIN')")` + dev admin seed + integrity/auth/type-mismatch exception handlers; (5) `ProductSpecifications` composable filters + convenience endpoints `/featured`, `/new-arrivals`, `/bestsellers`; (6) `@BatchSize(25)` N+1 mitigation + `@DataJpaTest` repository slice (8 tests) + Mockito service unit tests (9 tests) + updated `backend/README.md` with endpoint recipes. 19/19 tests pass, live-verified via 14+ curl scenarios across every filter axis.
- ✅ **Ticket B001 — Backend Foundation** — Spring Boot 3.4 on Java 21 baseline, Maven wrapper, dev+prod profiles, CORS, security scaffold, uniform `ApiResponse`/`ApiError` envelopes, `GlobalExceptionHandler`, actuator + friendly `/health`, `NUMERIC(19,4)` money, env-var config throughout.
- ✅ **Ticket 002 — Collections (v0.7.0)** — Collections index rebuilt as a 3-column luxury grid; new dynamic `/collections/[slug]` detail pages for all 6 collections (prerendered as SSG via `generateStaticParams`); `getCollection` / `getRelatedCollections` accessors with `notFound()` handling; `CollectionCard` enhanced with pieces count + explicit "Explore the Collection" CTA; enriched Collection schema with `tagline`, `story[]`, `signaturePieces[]`, `fabric`, `startingPrice`; sitemap now includes 6 detail URLs; per-collection metadata + OG images + canonical URLs + `CollectionPage` JSON-LD; primary detail-page CTA points at `/shop?collection={slug}` — ready to activate the moment Ticket 003 (Shop) lands
- ✅ **Homepage completed to production quality (v0.6.0)** — dynamic favicon + apple-icon (Next.js `ImageResponse` monogram), web app manifest, branded `not-found.tsx`, root `error.tsx`, `viewport` export with theme colours, `WebSite` JSON-LD, hero image LCP-tuned with `fetchPriority` + `object-position`, all sections `aria-label`'d
- ✅ Home page (Hero, Brand Statement, Featured Collections, Why Choose, Process, Testimonials, CTA)
- ✅ Home page hardened for production: page-specific `metadata`, canonical URL, JSON-LD Organization + LocalBusiness schema
- ✅ Editorial `BrandStatement` section — luxury-house tempo before commerce grid
- ✅ Testimonials anonymised to role + city (matches Hermès/Zegna convention; eliminates placeholder attribution)
- ✅ Testimonials markup upgraded to proper `<figure>` / `<blockquote>` / `<figcaption>` semantics
- ✅ ESLint configured (`next/core-web-vitals` + `next/typescript`); `npm run lint` passes with zero warnings
- ✅ About, Collections, Lookbook, Contact pages
- ✅ Navbar (sticky, transparent-over-hero, mobile drawer) + Footer
- ✅ Root SEO metadata, sitemap, robots, skip-to-content link, semantic `<main>`
- ✅ Centralised image asset registry (`src/lib/assets.ts`)
- ✅ Full folder architecture per `13_SYSTEM_ARCHITECTURE.md`
- ✅ Reusable `CollectionCard`, `SectionHeader`, `PageHeader`, `CTASection`, `BrandStatement`
- ✅ shadcn primitives (Button, Input, Textarea, Select, Label)
- ✅ Dependency conflict resolved (React 19 stable pin)
- ✅ Documentation reorganisation audit + new organisational docs

---

## In flight this week

- [ ] Documentation reorganisation execution (moves/renames per audit report)
- [ ] Vercel deployment
- [ ] Contact form → real inbox (Resend)
- [ ] Analytics + consent banner

---

## Up next (2-week horizon)

- [ ] Replace top 3 placeholders with real brand photography
- [ ] Author `docs/product/12_MEASUREMENT_FLOW.md`
- [ ] Author `docs/engineering/16_PAYMENT_ARCHITECTURE.md`
- [ ] Author `docs/ai/PROJECT_RULES.md` and `docs/ai/UI_RULES.md`
- [ ] Kick off Phase 2 discovery

---

## Metrics

| Metric | Value | Trend |
|---|---|---|
| Docs total | 35 | ↑ |
| Docs to archive | 9 | audit outcome |
| Docs to author | 15 | see `docs/README.md` |
| Code pages built | 5 / 5 | ✅ Phase 1 complete |
| Components built | 15 | ↑ |
| Placeholder images in use | 8 unique | to swap for brand assets |
| Lighthouse score (est.) | Not yet measured | pending deployment |
| Typecheck status | ✅ clean (`tsc --noEmit`) | — |
| Lint status | ✅ zero warnings (`next lint`) | — |
| Production build | ✅ 26 routes (unchanged in B005 — cart lives inside the existing `/cart` page) | — |
| Backend tests | ✅ 183/183 PASS (was 129 pre-B005; +54 new cart/mapper/issue-detector/merge/endpoint-matrix) | ↑ from 129 |
| Flyway migrations | V1–V6 (V6 carts + cart_items shipped in B005) | ↑ from V5 |
| Home HTML size | 3.24 kB | ✅ excellent |
| Home first-load JS | 157 kB | ✅ excellent |
| Collections index HTML | 514 B | ✅ excellent |
| Collection detail HTML (×6) | 514 B each | ✅ excellent |
| Favicon / apple-icon / manifest | ✅ all present | — |
| Branded 404 + error boundary | ✅ present | — |
| Customer shopping experience progress | 80% (Shop + PDP live; taxonomies wired; auth + accounts; cart end-to-end with guest→server merge per B005; orders + Paystack pending) | ↑ from 70% |
| v1.0 launch progress | **~97%** | ↑ from 96% |

---

## Risks (top 3)

See `docs/management/RISKS.md` for the full register.

1. **Photography delivery timeline** — placeholders in use across all pages. Impact on launch aesthetic if brand photography slips. *Mitigation:* Assets registry allows one-line swap when files arrive.
2. **Paystack integration complexity** — Phase 2 depends on it; no spec written yet. *Mitigation:* Write `16_PAYMENT_ARCHITECTURE.md` before any commerce code.
3. **Diaspora shipping / customs** — international bespoke fulfilment adds ops complexity. *Mitigation:* Scoped to Phase 3; discovery required with logistics partner.

---

## Decisions taken this period

| Decision | Rationale |
|---|---|
| Use `src/lib/assets.ts` as single image source | Enables swap-in of final photography without component changes |
| Pin React 19 stable (not RC) | Resolves framer-motion peer-dep conflict cleanly |
| Delete leftover `eazi-cut/` scaffold | Was serving default Next.js starter page and confusing dev workflow |
| Consolidate documentation into 8 folders | Current 35 files across flat structure was unnavigable |

---

## Change log links

- `docs/management/CHANGELOG.md` — engineering changelog
- `docs/management/ROADMAP.md` — forward plan
- `docs/management/BACKLOG.md` — prioritised feature list
