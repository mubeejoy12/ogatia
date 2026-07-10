# Eazi Cut — Master Context

> **Read this first.** This is the one-file summary AI assistants and new
> engineers must read before touching anything else. It is deliberately short.
> Depth lives in `docs/`.

---

## 1. Project Overview

**Eazi Cut** is a luxury African menswear brand delivering bespoke tailoring,
premium ready-to-wear, and wedding commissions. The digital platform is a
**luxury fashion house on the web** — not an ecommerce store, not a SaaS, not
a startup landing page. Every visual and interaction decision must earn a
₦500,000+ purchase.

The platform ships in three phases:

1. **Marketing website** *(current)* — Home, About, Collections, Lookbook, Contact
2. **Product catalogue + commerce** — categories, PDPs, cart, checkout, payments
3. **Bespoke tailoring engine** — measurements, order lifecycle, atelier ops

---

## 2. Client

| | |
|---|---|
| Business | **Eazi Cut** |
| Owner | **Mubarak Abubakar** |
| Delivery Partner | **Beejoy Technologies** (aka Mubeejoy Technologies) |
| Location | Lagos, Nigeria — serving Nigeria + African diaspora |

---

## 3. Business Goals

- Become **Africa's leading luxury tailoring house**, recognised globally.
- Enable customers anywhere in the world to commission bespoke pieces from Lagos.
- Digitise the tailoring pipeline end-to-end: measurement → cutting → fitting → delivery.
- Maintain a brand experience worthy of Tom Ford / Ozwald Boateng / Dior Homme.

**Non-goals:** volume ecommerce, discount-driven growth, generic SaaS UX,
"start-up" aesthetics.

---

## 4. Target Audience

Four primary personas (details in `docs/product/06_USER_PERSONAS.md`):

1. **The Executive** — senior professionals commissioning suits for boardrooms.
2. **The Groom** — high-value wedding parties needing coordinated tailoring.
3. **The Diaspora Gentleman** — Nigerians abroad wanting heritage tailoring shipped globally.
4. **The Ready-to-Wear Client** — style-driven customers buying finished pieces.

---

## 5. Tech Stack

| Layer | Choice | Status |
|---|---|---|
| Framework | **Next.js 15.1.4** (App Router) | active |
| Language | **TypeScript 5.7** (`strict: true`) | active |
| Styling | **Tailwind CSS 3.4** | active |
| Components | **shadcn/ui** primitives | active |
| Motion | **Framer Motion 11.15** | active |
| Icons | **lucide-react** | active |
| Fonts | **Playfair Display** (display) + **Inter** (body) via `next/font` | active |
| Colours | **ink `#0A0A0A`** · **ivory `#F5F1EA`** · **gold `#B8893E` / `#D4A85C` / `#8C6628`** | active |
| Images | Placeholder registry in `src/lib/assets.ts` | active |
| Backend | **Spring Boot 3** + **Java 21** + PostgreSQL 16 | Phase 3 |
| Auth | JWT (access + refresh) + RBAC | Phase 3 |
| Payments | **Paystack** | Phase 2 |
| File storage | **Cloudinary** (V2), AWS S3 (future) | Phase 2 |
| Hosting | **Vercel** (frontend), **Render** (backend), **Neon PostgreSQL** (DB) | per PROJECT_MANIFEST |

**Important:** the code-level design tokens above supersede any conflicting values in `docs/02_BRAND_GUIDELINES.md`. The brand doc will be reconciled in the documentation refactor.

---

## 6. Folder Structure

```
/                         repo root
├── README.md
├── MASTER_CONTEXT.md     ← this file
├── CLAUDE.md             ← AI entry point
├── docs/                 ← all specification
│   ├── README.md         ← documentation index
│   ├── business/  product/  engineering/  design/
│   ├── ai/  api/  assets/  management/
│   └── archive/
├── src/                  ← current Next.js app (Phase 1)
│   ├── app/              routes
│   ├── components/       layout, sections, ui primitives, motion
│   ├── features/         feature modules (e.g. contact)
│   ├── hooks/            reusable stateful hooks
│   ├── services/         data / API layer
│   ├── lib/              utils, site config, data, assets registry
│   └── types/            shared TS types
├── tasks/                task briefs
└── .github/              PR + issue templates
```

Full detail: `docs/engineering/13_SYSTEM_ARCHITECTURE.md`.

---

## 7. Current Progress

**Phase 1 — Marketing Website: ✅ shipped locally**

- ✅ Home (Hero, Featured Collections, Why Choose, Process, Testimonials, CTA)
- ✅ About
- ✅ Collections (editorial spreads)
- ✅ Lookbook (masonry gallery)
- ✅ Contact (form + atelier info, wired to service/hook/type layers)
- ✅ Navbar (sticky, transparent-over-hero, mobile drawer)
- ✅ Footer
- ✅ SEO metadata + sitemap + robots
- ✅ Centralised image asset registry (`src/lib/assets.ts`)
- ✅ Feature/hook/service/type folder structure per architecture spec
- ✅ Reusable `CollectionCard`
- ✅ shadcn primitives: Button, Input, Textarea, Select, Label
- ⏳ Deployment to Vercel — not started
- ⏳ Real brand photography — placeholders in use

**Phase 2 & 3:** not started.

Full breakdown: `docs/management/PROJECT_STATUS.md` and `docs/management/ROADMAP.md`.

---

## 8. Coding Standards

**TypeScript**
- `strict: true`; no `any` without justification.
- Prefer `type` for shapes, `interface` for public contracts.
- Component props always explicitly typed.

**React / Next.js**
- Server Components by default; `"use client"` only when needed.
- Use App Router conventions (`layout.tsx`, `page.tsx`, route groups).
- Data lives in `src/lib/data/*`, never inline in components.
- Images always go through `src/lib/assets.ts`.

**Styling**
- Only Tailwind + tokens defined in `tailwind.config.ts`.
- No inline `style={{}}` for anything design-system-owned.
- Colours: **ink / ivory / warm gold** only. No other hues.
- Typography: **Playfair Display** (display) + **Inter** (body). Nothing else.

**Motion**
- Framer Motion, editorial easing only.
- Respect `prefers-reduced-motion`.
- Never bounce, spring, or use SaaS-style micro-interactions.

**Accessibility**
- Semantic HTML first, ARIA second.
- Every interactive element keyboard-reachable.
- Every image has meaningful `alt` (stored with the asset).

---

## 9. AI Rules

Every AI assistant working in this repo must:

1. **Read `MASTER_CONTEXT.md` → `CLAUDE.md` → the relevant spec doc** before generating code.
2. **Never introduce SaaS / dashboard / startup patterns.** No purple gradients, no "get started" hero copy, no feature-grid three-columns-with-icons layouts.
3. **Never hardcode image URLs.** Add to `src/lib/assets.ts`.
4. **Never invent brand facts.** If it isn't in `docs/business/` or `docs/product/`, ask.
5. **Prefer editing existing files.** Extraction only when reuse is proven.
6. **Explain architecture decisions before implementation** for anything non-trivial.
7. **Never commit without the user asking.**
8. **Never `--force`, `--no-verify`, or `--legacy-peer-deps`** unless the user explicitly requests it.

Full ruleset: `docs/ai/PROJECT_RULES.md` + `docs/ai/UI_RULES.md`.

---

## 10. Documentation Rules

1. **One fact, one home.** Cross-link; do not restate.
2. **Docs travel with code.** Spec changes go in the same PR as implementation.
3. **Filename convention:** `NN_UPPER_SNAKE.md` or `UPPER_SNAKE.md`. No spaces, no dots inside filenames.
4. **Numbers are stable** once assigned. Insert new docs with the next free number.
5. **Update this file** whenever tech stack, scope, or phase progress changes.
6. **Keep this file ≤ 300 lines** so it fits in a single AI context turn.

---

## 11. Current Sprint — Launch Sprint

**Goal:** ship v1.0 (marketing site) to production by end of July 2026.

The MVP is intentionally scope-collapsed to what is achievable in ~3 weeks:

1. Deployment to Vercel on production domain
2. Contact form wired to Resend/Postmark inbox
3. Real photography for Hero + About-craft + Contact-atelier (min. 3 shots)
4. Analytics + NDPR consent banner + privacy policy page
5. Lighthouse ≥ 95 on all four scores
6. Cross-browser QA matrix

**Everything else in the current 35 documents is v2 or v3** — see `docs/management/ROADMAP.md`.

---

## 12. Next Tasks (this week)

1. **Approve documentation refactor** (this report) → execute file moves/renames.
2. **Reconcile brand doc with code** (fonts, colours, radii).
3. **Vercel project + preview environment.**
4. **Contact-form service:** replace 600 ms mock with Resend.
5. **Author `docs/engineering/17_DEPLOYMENT.md`** (Vercel + env vars + domains).
6. **Author `docs/assets/PLACEHOLDER_REGISTRY.md`** (which of the 22 slots still need real photography).

Full backlog: `docs/management/BACKLOG.md`.

---

## 13. Doc conflicts flagged (fix before Phase 2)

- `docs/02_BRAND_GUIDELINES.md` claims Cormorant Garamond + Manrope; code uses Playfair + Inter → **code wins.**
- `docs/02_BRAND_GUIDELINES.md` colours `#111111` / `#C8A045` differ from code tokens → **code wins.**
- `docs/PROJECT_MANIFEST.md` contains its content triplicated inside the file → **archive.**
- Three vision docs (`PROJECT_VISION`, `PRODUCT_VISION`, `01_PROJECT_VISION`) overlap → **consolidate into 01_.**
- Two brand docs, two architecture docs → **archive the short versions.**

See the full audit report for the complete list.
