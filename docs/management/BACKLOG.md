# Eazi Cut Backlog

**Last updated:** 2026-07-10
**Format:** MoSCoW (Must / Should / Could / Won't-yet)

Items are grouped by MoSCoW priority within each phase. Anything actively in
this sprint lives in `CURRENT_SPRINT.md`; anything shipped moves to
`CHANGELOG.md`.

---

## MUST HAVE (blocks Phase 1 launch)

### Deployment & operations
- [ ] **Deploy marketing site to Vercel** production + preview environments
- [ ] **Production domain** wired with SSL
- [ ] **Contact form → real inbox** (Resend or Postmark)
- [ ] **NDPR-compliant consent banner** + privacy policy page
- [ ] **Analytics** (Plausible or GA4 with consent gating)

### Content
- [ ] **Replace hero placeholder** with brand photography
- [ ] **Replace collection covers** (6 images) with commissioned shots
- [ ] **Real testimonials** — 3 named, consented client quotes
- [ ] **Real atelier photo** for About + Contact pages

### Documentation
- [ ] Execute file moves/renames per audit report
- [ ] Author `docs/ai/PROJECT_RULES.md`
- [ ] Author `docs/ai/UI_RULES.md`
- [ ] Author `docs/ai/CODING_STANDARDS.md`
- [ ] Author `docs/assets/PLACEHOLDER_REGISTRY.md`

### Quality gates
- [ ] Lighthouse ≥ 95 on all four scores (Performance, Accessibility, Best Practices, SEO)
- [ ] Manual QA matrix: iOS Safari · Android Chrome · Desktop Chrome · Desktop Safari · Firefox
- [ ] Axe accessibility scan — zero critical issues
- [ ] Meta OG + Twitter cards validated via LinkedIn/Twitter preview tools

---

## SHOULD HAVE (nice for launch, non-blocking)

- [ ] **Journal / editorial page** — long-form storytelling
- [ ] **Press page** — publications featuring Eazi Cut
- [ ] **Careers page** — brand notice to attract atelier talent
- [ ] **Newsletter signup** with double opt-in
- [ ] **Structured data (JSON-LD)** — Organization, LocalBusiness, BreadcrumbList
- [ ] **Static OG image generator** — dynamic per-collection OG cards
- [ ] **Sitemap ping to Google Search Console + Bing**
- [ ] **Uptime monitoring** (Better Uptime / Statuspage)
- [ ] **Error monitoring** (Sentry)
- [ ] **CI/CD** — GitHub Actions running typecheck + build on every PR
- [ ] **Pre-commit hooks** (Husky + lint-staged) for typecheck + prettier
- [ ] **Component library documentation** (`docs/design/20_COMPONENT_LIBRARY.md`)

---

## COULD HAVE (nice to have, Phase 2 candidates)

- [ ] Blur-hash placeholders on all `next/image`
- [ ] Dark-mode variant (evening browsing)
- [ ] Multi-currency price display (NGN / USD / GBP)
- [ ] Multi-language (English → French for Francophone diaspora)
- [ ] Live chat via WhatsApp Business API
- [ ] Instagram feed integration on home page
- [ ] Visual search ("find pieces like this")
- [ ] "Book a video consultation" scheduler
- [ ] Gift cards (invitation-only)

---

## WON'T HAVE YET (deferred to Phase 2 or later)

### Phase 2 scope
- Full product catalogue + PDPs
- Cart + checkout + Paystack
- User accounts + authentication
- Order history + tracking
- Wishlist
- Product search + filters

### Phase 3 scope
- Bespoke commission flow
- Measurement capture (guided + video)
- Fitting scheduling
- Atelier operations dashboard
- Staged payments (deposit + balance)

### Never (explicit non-goals)
- ❌ Discount codes as a growth channel
- ❌ Marketplace / third-party sellers
- ❌ Subscription boxes
- ❌ Fast-fashion drops / limited-edition urgency tactics
- ❌ Points-based loyalty program
- ❌ Popup email captures
- ❌ Chatbots on the customer-facing site

---

## Documentation backlog

Reflects the "missing docs" list from the audit. Prioritised so blockers ship first.

| Priority | Document | Why |
|---|---|---|
| Must | `docs/ai/PROJECT_RULES.md` | AI assistants need consolidated behaviour rules |
| Must | `docs/ai/UI_RULES.md` | Front-end aesthetic guardrails |
| Must | `docs/ai/CODING_STANDARDS.md` | Prevents style drift across AI sessions |
| Must | `docs/assets/PLACEHOLDER_REGISTRY.md` | Track what still needs real photography |
| Should | `docs/product/12_MEASUREMENT_FLOW.md` | Blocks Phase 3 design work |
| Should | `docs/engineering/16_PAYMENT_ARCHITECTURE.md` | Blocks Phase 2 commerce work |
| Should | `docs/engineering/15_AUTHENTICATION.md` | Blocks Phase 2 accounts |
| Should | `docs/api/API_SPECIFICATION.md` | Front-end contract for Phase 2 |
| Should | `docs/design/19_DESIGN_SYSTEM.md` (expanded) | Tokens as JSON |
| Should | `docs/design/20_COMPONENT_LIBRARY.md` | Inventory + usage |
| Should | `docs/design/21_MOTION_GUIDELINES.md` | Motion patterns + easing |
| Could | `docs/engineering/17_DEPLOYMENT.md` | Formalise Vercel + backend hosting |
| Could | `docs/engineering/18_TESTING_STRATEGY.md` | Formalise QA approach |
| Could | `docs/ai/BACKEND_RULES.md` | Spring Boot rules for Phase 3 |
| Could | `docs/api/WEBHOOKS.md` | Paystack + logistics inbound webhooks |
| Could | `docs/assets/ASSET_INVENTORY.md` | Photography / fonts / logos + licences |
| Could | `docs/management/BUGS.md` | Known bugs (empty until launch) |
| Could | `CONTRIBUTING.md` | Contribution rules |
| Could | `SECURITY.md` | Disclosure policy |
| Could | `.github/PULL_REQUEST_TEMPLATE.md` | PR checklist |

---

## Estimation legend

| Effort | Meaning |
|---|---|
| XS | < ½ day |
| S | ½ – 1 day |
| M | 2 – 3 days |
| L | 1 week |
| XL | 2+ weeks |

Estimates will be added as items are pulled into a sprint.
