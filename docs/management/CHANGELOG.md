# Changelog

All notable changes to Eazi Cut follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Dates use ISO 8601. Unreleased sits at the top; each release moves it below.

---

## [Unreleased]

### Planned
- Deploy v0.5.0 to Vercel production
- Wire `services/contact.ts` to Resend (real inbox)
- Replace top 3 placeholder images with commissioned photography
- NDPR consent banner + `/privacy` + `/terms` pages
- Plausible analytics

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
