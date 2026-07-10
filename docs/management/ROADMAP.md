# Eazi Cut Roadmap — Now to Launch

**Last updated:** 2026-07-10

This roadmap consolidates the earlier `ROADMAP.md`, `SPRINT_1.md`, and
`management/1. MILESTONES.md` into one forward plan.

---

## Guiding sequence

```
Phase 1  →  Phase 2  →  Phase 3  →  Post-launch
Marketing   Catalogue    Bespoke     Growth
(now)       Q4 2026      Q1 2027     ongoing
```

Every phase must ship a version that is production-usable on its own. No phase
starts until the previous is deployed and stable.

---

## Phase 1 — Marketing Website  🟢 in progress

**Goal:** convince a first-time luxury customer to enquire.

### 1A. Local build ✅
- ✅ Home, About, Collections, Lookbook, Contact
- ✅ Navbar, Footer, Hero, Featured Collections, Why Choose, Process, Testimonials, CTA
- ✅ SEO metadata, sitemap, robots.txt
- ✅ Reusable components, feature/hook/service/type architecture
- ✅ Centralised image registry

### 1B. Documentation ⏳ this session
- ✅ MASTER_CONTEXT.md
- ✅ CLAUDE.md (root)
- ✅ docs/README.md
- ✅ PROJECT_STATUS, ROADMAP, BACKLOG (this doc + siblings)
- [ ] Physical file moves per audit
- [ ] Author 15 missing docs (see docs/README.md)

### 1C. Deployment ⏳ next
- [ ] Vercel project + preview environment
- [ ] Production domain wired
- [ ] Contact form → Resend inbox
- [ ] Analytics + NDPR-compliant consent banner
- [ ] Lighthouse audit ≥ 95 on all four scores
- [ ] Manual QA on iOS Safari, Android Chrome, Desktop Chrome, Desktop Safari, Firefox

### 1D. Content ⏳ pending assets
- [ ] Replace top 3 hero images with real brand photography
- [ ] Replace all collection cards with commissioned shots
- [ ] Replace lookbook with editorial series
- [ ] Populate testimonials with named, consented clients

**Exit criteria:** brand-photographed site live on production domain; contact
form receiving enquiries reliably; three real testimonials published.

---

## Phase 2 — Product Catalogue & Commerce  ⚪ Q4 2026

**Goal:** enable a customer to purchase ready-to-wear online without speaking to anyone.

### 2A. Foundations
- [ ] Author `docs/engineering/15_AUTHENTICATION.md`
- [ ] Author `docs/engineering/16_PAYMENT_ARCHITECTURE.md`
- [ ] Author `docs/api/API_SPECIFICATION.md`
- [ ] Author `docs/product/12_MEASUREMENT_FLOW.md`
- [ ] Content model: CMS vs. code-owned

### 2B. Catalogue
- [ ] Category pages with faceted filters
- [ ] Product Detail Pages (PDP) with editorial photography, sizing, fabric detail
- [ ] Search (typeahead + full results)
- [ ] Wishlist (guest + logged-in)

### 2C. Commerce
- [ ] Cart (persistent, guest + user)
- [ ] Checkout flow (address, shipping method, payment)
- [ ] Paystack integration (Naira + card + international where supported)
- [ ] Order confirmation email + receipt
- [ ] Order history + status page for logged-in users
- [ ] Refunds + exchanges workflow

### 2D. Accounts
- [ ] Sign up / sign in (email + optional social)
- [ ] Profile: measurements-on-file, shipping addresses
- [ ] Password reset

**Exit criteria:** a customer in Lagos and a customer in London can each buy a
ready-to-wear piece end-to-end, receive it, and initiate a return if needed.

---

## Phase 3 — Bespoke Tailoring Engine  ⚪ Q1 2027

**Goal:** commission a bespoke suit remotely.

### 3A. Measurements
- [ ] Guided measurement capture (photo + video-assisted)
- [ ] Save measurement profiles per user
- [ ] Book an in-atelier fitting (Lagos)
- [ ] Book a remote fitting (video)

### 3B. Commissioning
- [ ] Bespoke commission flow (fabric → style → measurements → deposit → review → confirm)
- [ ] Staged payments (deposit + balance)
- [ ] Fitting scheduling
- [ ] Delivery logistics (domestic + international)

### 3C. Atelier operations
- [ ] Atelier dashboard (order queue, fitting calendar, workshop status)
- [ ] Cutter / tailor assignment
- [ ] Alteration tracking

**Exit criteria:** a diaspora client can commission a bespoke wedding suit from
first click to delivery without visiting Lagos in person.

---

## Post-launch  ⚪ Ongoing

- International shipping optimisation
- Referral program (invitation-only, per luxury norms)
- Editorial content: journal / lookbook cadence
- Loyalty (invisible loyalty — no points, curated invitations)
- Bulk wedding commissions (groomsmen packages)
- Corporate native-wear commissions
- Expansion beyond Nigeria

---

## Non-goals (explicitly out of scope, ever)

- ❌ Discount coupons, flash sales, promo codes as a growth channel
- ❌ Marketplace / third-party sellers
- ❌ Subscription boxes
- ❌ Fast-fashion aesthetics
- ❌ SaaS admin UX for the customer-facing side

---

## Dependencies & milestones

| Milestone | Depends on | Target |
|---|---|---|
| M1 · Marketing live | Phase 1B + 1C + 1D | Aug 2026 |
| M2 · Commerce spec complete | `15_AUTH` + `16_PAYMENTS` + `API_SPEC` docs | Sep 2026 |
| M3 · RTW commerce live | Phase 2 | Dec 2026 |
| M4 · Bespoke MVP live | Phase 3A + 3B | Mar 2027 |
| M5 · Full atelier ops | Phase 3C | Q2 2027 |
