# Changelog

All notable changes to Eazi Cut follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Dates use ISO 8601. Unreleased sits at the top; each release moves it below.

---

## [Unreleased]

### Planned
- Deploy v0.10.0 to Vercel production alongside a hosted backend instance
- Wire `services/contact.ts` to Resend (real inbox); read `?collection=` query on Contact form to pre-fill enquiry
- Replace top 3 placeholder images with commissioned photography
- NDPR consent banner + `/privacy` + `/terms` pages
- Plausible analytics
- Backend Order module
- Auth hardening pass: email verification, password reset, refresh-token reuse detection, Redis-backed rate limiter for horizontal scale
- Navbar sign-in / sign-out affordance

---

## [0.10.0] — 2026-07-30 — Ticket B004 · Authentication & Authorization

Production-ready auth. Retires the B001 scaffold (`spring.security.user.*`
in-memory user + `permitAll` filter chain + HTTP Basic on the wire) and
replaces it with DB-backed users, BCrypt password hashing, JWT bearer
tokens (15-min access) + rotating refresh tokens (7-day, HttpOnly
cookie), per-IP + per-email login rate limiting, and a secure-by-default
`.anyRequest().authenticated()` filter chain with an explicit public
allowlist. Frontend gets login / register / account pages, an
in-memory access-token `AuthContext`, and a middleware gate for
`/account`.

Eight atomic stages, one commit each:

### Added — backend
- **`V4__users.sql`** — users table with `email` + `email_lower`
  (case-insensitive unique via `ux_user_email_lower` — same portable
  pattern V2/V3 established), BCrypt-sized `password_hash`, VARCHAR
  `role` with `idx_user_role`, `enabled` flag, standard audit columns.
- **`V5__refresh_tokens.sql`** — refresh tokens with FK to users
  (ON DELETE CASCADE), UNIQUE(token_hash), `issued_at`/`expires_at`/
  `revoked_at`. Only the SHA-256 hash of the raw token is stored.
- **`users` package** — `User` entity (`@PrePersist` sync of
  `email_lower`), `Role` enum (`CUSTOMER` / `ADMIN` with `authority()`
  helper returning `ROLE_*`), `UserRepository`, `UserResponse` DTO
  that deliberately omits `passwordHash`/`emailLower`/`enabled`,
  `UserMapper` (one-way `toResponse` only).
- **`users/security`** — `UserDetailsAdapter` (record adapting the
  entity to Spring's contract), `JpaUserDetailsService` (DB-backed,
  normalises input, `@Transactional(readOnly=true)`).
- **`auth/validation`** — `@ValidPassword` custom Bean-Validation
  annotation + `PasswordValidator` (8–128 length, no forced
  complexity, ~18-entry blocklist including `password`, `qwerty`,
  `eazicut`).
- **`auth` package** — `RegisterRequest`, `LoginRequest`,
  `LoginResponse` (carries `accessToken` + `expiresInSeconds` +
  `user`; `refreshToken` field marked `@JsonIgnore` — never leaves in
  JSON). `DuplicateEmailException`, `InvalidCredentialsException`,
  `TooManyLoginAttemptsException`. `AuthService` with
  `register/login/refresh/logout/me` — email normalisation,
  constant-time BCrypt call even on unknown email (closes timing
  side-channel), two-layer uniqueness (service probe + DB backstop),
  rate limiter integration.
- **`auth/jwt`** — `JwtProperties` (`@ConfigurationProperties`),
  `JwtService` (HS256 issue + parse; claims `sub`, `email`, `role`,
  `iss`, `iat`, `exp`), `JwtAuthenticationFilter`
  (`OncePerRequestFilter`; Bearer → `SecurityContext` with no DB
  hit). JJWT 0.12.6.
- **`auth/refresh`** — `RefreshToken` entity (lazy `@ManyToOne` User),
  `RefreshTokenRepository`, `RefreshTokenService` (issue: 256-bit
  base64url random; rotate: revoke old + mint new atomically inside
  one `@Transactional`; revoke: idempotent). `RefreshCookies` — one
  central place for the cookie shape: `eazicut_refresh` (HttpOnly,
  Secure, SameSite=Lax, `Path=/api/v1/auth`) + companion
  `eazicut_session=1` presence marker (non-HttpOnly, `Path=/`) so
  the frontend middleware has something it can read.
- **`auth/ratelimit`** — `LoginRateLimiter` interface +
  `InMemoryLoginRateLimiter` (5 failures / 15 min rolling window,
  per-IP AND per-email; success resets email counter only).
- **`auth/controller/AuthController`** — `POST /auth/register`,
  `/login`, `/refresh`, `/logout` (all public, on the allowlist);
  `GET /auth/me` (authenticated).
- **`users/bootstrap/DevAdminSeeder`** — `@Profile("dev")`
  `ApplicationRunner`. Reads `eazicut.dev-admin.{email,password}`
  (bindable to `EAZICUT_DEV_ADMIN_EMAIL` / `EAZICUT_DEV_ADMIN_PASSWORD`;
  defaults `admin@eazicut.local` / `admin`). Idempotent.

### Changed — backend
- **`SecurityConfig`** — added `PasswordEncoder` (BCrypt) +
  `AuthenticationManager` beans. **Flipped the filter chain default
  from `.anyRequest().permitAll()` to `.anyRequest().authenticated()`**
  with an explicit public allowlist: `/health`, `/actuator/health`,
  `/actuator/info`, `GET /products/**`, `GET /categories/**`,
  `GET /collections/**`, `POST /auth/{register,login,refresh,logout}`.
  Registered `JwtAuthenticationFilter` before
  `UsernamePasswordAuthenticationFilter`. **Removed `httpBasic`.**
  Set `HttpStatusEntryPoint(UNAUTHORIZED)` so missing-credential
  requests return 401 rather than the framework's default 403.
  CSRF stays disabled — bearer JWT + SameSite=Lax refresh cookie +
  no ambient session make CSRF inapplicable; documented at length.
- **`GlobalExceptionHandler`** — added handlers for
  `InvalidCredentialsException` (401, `invalid_credentials`) and
  `TooManyLoginAttemptsException` (429 + `Retry-After` header).
- **`ProductRepository`** — no changes required.
- **`application-dev.yml`** — retired the `spring.security.user.*`
  block; added `eazicut.dev-admin.*` and dev-only JWT secret default.
- **`application.yml`** — added `eazicut.jwt.{issuer,
  access-token-ttl, refresh-token-ttl}` with env-var overrides.
- **`application-prod.yml`** — added `eazicut.jwt.secret:
  ${EAZICUT_JWT_SECRET}` with no default — pod fails to boot if the
  env var isn't set (safe outcome).
- **`pom.xml`** — `io.jsonwebtoken:jjwt-{api,impl,jackson}` at 0.12.6.

### Added — frontend
- **`src/types/api/auth.ts`** — mirrors the backend DTOs (`Role`,
  `ApiUserResponse`, `ApiRegisterRequest`, `ApiLoginRequest`,
  `ApiLoginResponse`).
- **`src/lib/api/auth.ts`** — typed `register/login/refresh/logout/me`
  using its own POST helper with `credentials: "include"` so the
  HttpOnly refresh cookie flows. Deliberately separate from the
  generic `apiGet` so unrelated `/products` calls stay cookie-free.
- **`src/features/auth/AuthContext.tsx`** — `AuthProvider` + `useAuth`
  + `useAccessToken`. Access token lives in a React ref
  (never localStorage → XSS-safe). Mount-time silent refresh so a
  page reload doesn't force a re-login.
- **`src/app/(auth)/login/page.tsx`** — luxury "Sign in to the
  Atelier". `useSearchParams` in a `Suspense` boundary (required for
  static prerender). `?next=` validated as same-origin-absolute
  (prevents open-redirect abuse).
- **`src/app/(auth)/register/page.tsx`** — luxury "Open an account".
  Minimal per D3 (email + password + optional display name).
  Auto-signs-in on success. Maps 409 → "already exists", 429 →
  rate-limit copy.
- **`src/app/account/page.tsx`** — luxury stub. Client-guarded:
  bounces to `/login?next=/account` if `AuthContext` settles to
  `anonymous`. Shows email + role + sign-out button.
- **`src/middleware.ts`** — reads `eazicut_session` presence cookie;
  unauth → 302 `/login?next=<original>`. Matcher scoped to
  `/account/:path*` only.

### Changed — frontend
- **`src/app/layout.tsx`** — wraps `CartProvider` in `AuthProvider`.

### Tests
- **Backend 129 total** (was 19 before B004; +110 new):
  `UserRepositoryTest` (4), `JpaUserDetailsServiceTest` (3),
  `PasswordValidatorTest` (8), `AuthServiceTest` (15 — register + login
  + refresh + logout + me + normalisation + constant-time), `JwtServiceTest`
  (6), `EndpointAuthorizationTest` (18 — every mapped endpoint × auth
  level), `RefreshTokenServiceTest` (10 slice), `FullAuthFlowTest` (1
  end-to-end MockMvc loop: register → login → me → refresh → replay-old
  → me-with-new → logout → dead → idempotent), `InMemoryLoginRateLimiterTest`
  (8).

### Verified end-to-end (33/33)
Live gauntlet against a fresh backend on H2 (ordered so rate-limit
saturation runs last):

| # | Check | Result |
|---|---|---|
| U1×9 | All 9 write endpoints anonymous → 401 (POST/PUT/DELETE on products, categories, collections) | ✅ |
| U2 | Pre-B004 `admin:admin` creds → 401 | ✅ (in-memory store gone) |
| U3 | HTTP Basic itself → 401 | ✅ (mechanism removed) |
| U4 | POST /auth/register → 201 | ✅ |
| U5 | POST /auth/login → 200 | ✅ |
| U6 | Short password → 400 · blocklisted password → 400 | ✅ |
| U7 | 6th failed login attempt → 429 + Retry-After header | ✅ |
| U8 | Wrong password → 401 (BCrypt verified) | ✅ |
| Loop | admin login → Bearer /categories 201 → /me 200 → /refresh rotates → old cookie 401 → new Bearer works → logout 204 → post-logout 401 | ✅ |
| Roles | CUSTOMER Bearer → admin write → 403 (not 401 — authenticated but not authorised) | ✅ |
| /me | valid Bearer → 200; invalid Bearer → 401 | ✅ |
| Public | GET /products, /categories, /collections, /health anonymous → 200 | ✅ |
| Dup | duplicate registration → 409 | ✅ |

### Known limitations & deferred hardening
- **Email verification** — deferred per D3. A registration succeeds
  immediately with no inbox check.
- **Password reset** — deferred. Only path today is: contact atelier.
- **MFA** — deferred.
- **Refresh-token reuse detection** — today, replaying a rotated
  cookie returns 401 but doesn't revoke the whole "family"; a
  follow-up ticket can wire that once telemetry is in place.
- **Rate limiter is in-memory single-instance** — the
  `LoginRateLimiter` interface is Redis-ready; horizontal scale
  needs a shared-store implementation.
- **Frontend has no bearer-attaching wrapper on the generic `apiGet`
  with silent-refresh-on-401** — no non-auth caller today needs it
  (product/category/collection reads are public). Add when the first
  bearer-requiring `/api/v1/…` call outside `/auth/*` lands.
- **Navbar sign-in/sign-out affordance** — deferred to polish. The
  `/login`, `/register`, `/account` pages are directly navigable.

### Build metrics (v0.10.0)

| | Value |
|---|---|
| Backend tests | 129/129 PASS (was 19 before B004; +110 new) |
| Frontend routes | 26 (added `/login`, `/register`, `/account`, `Middleware`) |
| Frontend typecheck | ✅ clean |
| Frontend lint | ✅ zero warnings |
| Frontend production build | ✅ compiles |
| Flyway migrations | V1–V5 |

---

## [0.9.0] — 2026-07-25 — Ticket B003 · Category & Collection reference-data

Full production-ready reference-data system on the backend. Both taxonomies
have REST CRUD, case-insensitive name uniqueness with a DB-level backstop,
domain-level in-use guards on delete, and a dev-only idempotent seeder
that ensures the six categories and six collections the frontend expects
are present on every dev boot. The frontend's `?category=` and
`?collection=` filter axes — wired end-to-end in F003 but previously
un-testable — are now live-verified positive.

### Added — backend
- **Category REST** — `/api/v1/categories` (GET list, GET /{id}, GET /slug/{slug} public; POST/PUT/DELETE admin). `CategoryRequest` / `CategoryResponse` records, `CategoryMapper` (MapStruct with IGNORE-null on update), `CategoryService` (create/read/update/delete/list). Reads `@Transactional(readOnly=true)`. Default list sort `name ASC`. Slug + name uniqueness probed on the service layer; DB-level unique constraint on `LOWER(name)` via V2 migration.
- **Collection REST** — `/api/v1/collections` — direct parallel of Category. V3 migration for `collections.name_lower` + unique index.
- **Case-insensitive name uniqueness (two layers)** —
  - Service: `existsByNameIgnoreCase` probe + name normalisation (trim + collapse internal whitespace) before every save. Duplicate → 409 with a helpful message ("A category with the name 'Suits' already exists.").
  - DB: `name_lower` column populated by `@PrePersist`/`@PreUpdate` on the entity; unique index on that column. Portable across H2 v2 and PostgreSQL (functional indexes and DB-generated columns disagree in syntax between the two — a plain column was the only clean shared path).
- **In-use guards on delete** — `CategoryInUseException` / `CollectionInUseException` (both 409). Raised *before* the DB delete, with the exact product count in the message ("Cannot delete collection 'lagos-heritage': 1 product still references it."). Never relies on a generic `DataIntegrityViolationException`.
- **Reference-data seeder** — `com.eazicut.api.reference.ReferenceDataSeeder`, `@Component @Profile("dev")` implementing `ApplicationRunner`. Idempotent (per-row `existsBySlug` probe). Seeds:
  - 6 categories: `suits`, `shirts`, `trousers`, `outerwear`, `native`, `accessories` — slugs match `CATEGORY_SLUG` in `src/features/shop/backendFilter.ts` (`name.toLowerCase()`).
  - 6 collections: `the-onyx-bespoke`, `ivory-wedding`, `lagos-heritage`, `the-essentials`, `the-noir-tuxedo`, `diaspora` — verbatim from `src/lib/data/collections.ts`.
  - Never seeds prod or test (profile-guarded). Mutable reference data stays out of Flyway.
- **`ProductRepository`** — `countByCategoryId(UUID)` and `countByCollectionId(UUID)` — feeding the in-use guards on both services.

### Added — frontend
- **`src/types/api/category.ts`, `src/types/api/collection.ts`** — mirror the backend `CategoryResponse` and `CollectionResponse` records verbatim.
- **`src/lib/api/categories.ts`, `src/lib/api/collections.ts`** — typed fetchers (`fetchCategories`, `fetchCategoryBySlug`, `fetchCollections`, `fetchCollectionBySlug`) using the existing `apiGet` client + envelope conventions.

### Changed — frontend (hardening, no redesign)
- **`src/features/shop/backendFilter.ts`** — removed three dead re-exports (`KNOWN_COLLECTION_SLUGS`, `KNOWN_COLLECTION_NAMES`, `KNOWN_SORT_MODES` had zero external callers) and the mock `import { collections } from "@/lib/data/collections"` that was only feeding one of them. Shop flow now has one less line of coupling to the mock.
- **`src/features/shop/filters.ts`** — `parseFilters` now emits `console.warn` when the URL contains an unknown category or collection value. Behaviour unchanged (unknown values still fall to `null`, safe empty-state renders), but merchandiser typos and stale-link drift are now visible in dev consoles instead of silently absorbed.

### Backend tests
- **CategoryServiceTest** — 12 Mockito unit tests (happy paths, duplicate slug/name, whitespace normalisation, update no-op skip, update slug/name change probe, delete blocked-by-products, delete happy, delete unknown, getBySlug unknown, normaliseName helper).
- **CategoryRepositoryTest** — 5 `@DataJpaTest` slice tests (findBySlug, existsBySlug, existsByNameIgnoreCase across cases, V2 unique index on `name_lower`, V1 slug uniqueness).
- **CollectionServiceTest** — 12 unit tests (direct parallel).
- **CollectionRepositoryTest** — 5 slice tests (direct parallel; V3 unique index).
- **ReferenceDataSeederTest** — 3 behavioural tests with hand-written fake repositories (first-run seeds all 12; second-run no-op; partial pre-existing state seeds only missing rows without overwriting descriptions).

### Verified end-to-end (14/14)
Live gauntlet against a fresh backend on H2 with the seeded taxonomy and
6 products assigned to real categories + collections:

| # | Check | Result |
|---|---|---|
| 1  | GET /api/v1/categories anonymous | 200 |
| 2  | GET /api/v1/collections anonymous | 200 |
| 3  | Seeded categories count | 6 |
| 4  | Seeded collections count | 6 |
| 5  | `?category=Suits` narrows | 4 products |
| 6  | `?category=Shirts` narrows | 1 |
| 7  | `?category=Outerwear` narrows | 1 |
| 8  | `?collection=the-onyx-bespoke` narrows | 3 |
| 9  | `?collection=ivory-wedding` narrows | 1 |
| 10 | `?collection=diaspora` narrows | 2 |
| 11 | Combined `?category=Suits&collection=the-onyx-bespoke` | 2 (correctly excludes Ink Overcoat which is Outerwear) |
| 12 | Related products on `/shop/onyx-two-piece` PDP | 2 distinct related pieces from same collection |
| 13 | Unknown `?category=Nonexistent` | dropped safely, all 6 shown, `console.warn` logged |
| 14 | Unknown `?collection=nope-slug` | dropped safely, all 6 shown, `console.warn` logged |

### Known limitations & tech debt
- **Frontend taxonomy is still static.** The `productCategories` const and `collectionSlugs` const are the frontend's source of truth for URL validation. If the atelier adds a new category via the admin API, the frontend won't render it in the toolbar or accept it in the URL until the const is updated in code. Dynamically fetching the taxonomy on Shop load is deferred — the `console.warn` gives visibility for now.
- **Category/Collection admin UI is CLI-only.** All writes go via the `/api/v1/{categories,collections}` REST endpoints with HTTP Basic (`admin/admin` in dev). A proper admin surface arrives with the auth ticket.
- **Concurrent dev-boot race in the seeder** is documented in the class Javadoc — the DB unique constraints on `slug` and `name_lower` are the backstop; the loser's INSERT rolls back cleanly.

### Build metrics (v0.9.0)

| Route | Type | Size | First Load JS |
|---|---|---|---|
| `/shop` | ƒ Dynamic | 8.95 kB | 163 kB |
| `/shop/[slug]` | ƒ Dynamic | 3.36 kB | 158 kB |
| `/sitemap.xml` | ƒ Dynamic | 140 B | 103 kB |
| Total routes | 22 | — | — |
| Backend tests | 56/56 PASS (was 19 at F003 close; +37) | — | — |
| Typecheck | ✅ clean | — | — |
| Lint | ✅ zero warnings | — | — |
| Production build | ✅ compiles | — | — |

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
