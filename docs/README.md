# Eazi Cut Documentation

The complete written knowledge base for Eazi Cut — a luxury African menswear
platform combining bespoke tailoring, ready-to-wear, and wedding commissions.

If you are an AI assistant (Claude, Codex, Cursor, ChatGPT) or a new engineer,
**read `/MASTER_CONTEXT.md` first**, then `/CLAUDE.md`, then return here for
depth on any specific area.

---

## How this repo is organised

```
docs/
├── README.md              ← you are here
├── business/              WHY we exist and WHO we serve
├── product/               WHAT we're building and HOW users experience it
├── engineering/           HOW the system is built
├── design/                HOW it looks, feels, and moves
├── ai/                    HOW AI assistants must behave in this repo
├── api/                   HTTP contract for the platform
├── assets/                Photography, logos, fonts, placeholders
├── management/            Roadmap, backlog, status, releases
└── archive/               Superseded documents kept for history
```

Numbering runs across the whole knowledge base (01–21) so cross-referencing
is unambiguous. Files without a number (in `ai/`, `api/`, `management/`) are
meta or operational and don't slot into the sequence.

---

## Business (01–04)

| # | Document | What it is |
|---|---|---|
| 01 | [`business/01_PROJECT_VISION.md`](business/01_PROJECT_VISION.md) | Mission, vision, target customers, business goals |
| 02 | [`business/02_BRAND_GUIDELINES.md`](business/02_BRAND_GUIDELINES.md) | Voice, colour, typography, imagery, motion tone |
| 03 | [`business/03_BUSINESS_REQUIREMENTS.md`](business/03_BUSINESS_REQUIREMENTS.md) | BRD — outcomes the business needs |
| 04 | [`business/04_COMPETITOR_ANALYSIS.md`](business/04_COMPETITOR_ANALYSIS.md) | Who else is in the market and how we differ |

---

## Product (05–12)

| # | Document | What it is |
|---|---|---|
| 05 | [`product/05_PRODUCT_PRINCIPLES.md`](product/05_PRODUCT_PRINCIPLES.md) | Non-negotiable principles for every product decision |
| 06 | [`product/06_USER_PERSONAS.md`](product/06_USER_PERSONAS.md) | Who our customers are |
| 07 | [`product/07_CUSTOMER_JOURNEY.md`](product/07_CUSTOMER_JOURNEY.md) | Awareness → advocacy touchpoints |
| 08 | [`product/08_EMOTIONAL_JOURNEY.md`](product/08_EMOTIONAL_JOURNEY.md) | What the customer *feels* at each step |
| 09 | [`product/09_INFORMATION_ARCHITECTURE.md`](product/09_INFORMATION_ARCHITECTURE.md) | Sitemap, taxonomy, navigation |
| 10 | [`product/10_USER_FLOWS.md`](product/10_USER_FLOWS.md) | Task-level flows (browse, order, book, pay) |
| 11 | [`product/11_FEATURE_REQUIREMENTS.md`](product/11_FEATURE_REQUIREMENTS.md) | Feature-by-feature specs |
| 12 | `product/12_MEASUREMENT_FLOW.md` *(to write)* | The measurement-capture experience — our differentiator |

---

## Engineering (13–18)

| # | Document | What it is |
|---|---|---|
| 13 | [`engineering/13_SYSTEM_ARCHITECTURE.md`](engineering/13_SYSTEM_ARCHITECTURE.md) | Runtime, services, boundaries |
| 14 | [`engineering/14_DATABASE_DESIGN.md`](engineering/14_DATABASE_DESIGN.md) | Schema, indexes, migrations |
| 15 | `engineering/15_AUTHENTICATION.md` *(to write)* | Sign-in, sessions, JWT, RBAC |
| 16 | `engineering/16_PAYMENT_ARCHITECTURE.md` *(to write)* | Paystack integration, webhooks, refunds, split payouts |
| 17 | `engineering/17_DEPLOYMENT.md` *(to write)* | Vercel, backend hosting, environments |
| 18 | `engineering/18_TESTING_STRATEGY.md` *(to write)* | Unit / integration / E2E / manual |

---

## Design (19–21)

| # | Document | What it is |
|---|---|---|
| 19 | `design/19_DESIGN_SYSTEM.md` *(move + expand)* | Tokens, radii, spacing, elevation |
| 20 | `design/20_COMPONENT_LIBRARY.md` *(to write)* | Inventory of shadcn primitives and custom components |
| 21 | `design/21_MOTION_GUIDELINES.md` *(to write)* | Framer Motion patterns, easing curves, reduced-motion |

---

## AI (meta)

| Document | What it is |
|---|---|
| [`ai/CLAUDE.md`](ai/CLAUDE.md) | Canonical AI operating instructions. Root `/CLAUDE.md` points here. |
| `ai/PROJECT_RULES.md` *(to write)* | Non-negotiable behaviour rules for AI assistants |
| `ai/UI_RULES.md` *(to write)* | Front-end aesthetic do's and don'ts |
| `ai/BACKEND_RULES.md` *(to write)* | Rules for Spring Boot backend work (Phase 3) |
| `ai/CODING_STANDARDS.md` *(to write)* | TS / React / Java conventions |

---

## API

| Document | What it is |
|---|---|
| `api/API_SPECIFICATION.md` *(move + expand from engineering/13)* | REST endpoints, request/response shapes |
| `api/WEBHOOKS.md` *(to write)* | Inbound webhooks (Paystack, logistics) |

---

## Assets

| Document | What it is |
|---|---|
| `assets/ASSET_INVENTORY.md` *(to write)* | Photography, logos, fonts, licence status |
| `assets/PLACEHOLDER_REGISTRY.md` *(to write)* | Mirror of `src/lib/assets.ts` — which placeholders remain |

---

## Management (operational)

| Document | What it is |
|---|---|
| [`management/PROJECT_STATUS.md`](management/PROJECT_STATUS.md) | Weekly status — where the project is today |
| [`management/ROADMAP.md`](management/ROADMAP.md) | Now → launch, phase by phase |
| [`management/BACKLOG.md`](management/BACKLOG.md) | Must / Should / Nice / Future |
| [`management/CHANGELOG.md`](management/CHANGELOG.md) | Keep-a-Changelog format |
| [`management/RISKS.md`](management/RISKS.md) | Risk register |
| `management/BUGS.md` *(to write)* | Known bugs |
| [`management/CURRENT_SPRINT.md`](management/CURRENT_SPRINT.md) | Active sprint's scope and status |
| [`management/RELEASE_CHECKLIST.md`](management/RELEASE_CHECKLIST.md) | Gate to production for every release |

---

## Archive

Files that have been superseded. Not deleted — preserved for history.
See [`archive/`](archive/).

---

## Documentation rules

1. **Every new spec is a PR.** Docs travel with code changes.
2. **No orphaned facts.** If it's true about the product, it lives in exactly one doc; everything else links.
3. **Never break MASTER_CONTEXT.** Any change to project scope, tech stack, or roadmap must update `/MASTER_CONTEXT.md`.
4. **Numbers are stable.** Once a doc has a number, it keeps it. Add gaps rather than renumber.
5. **Filename convention:** `NN_UPPER_SNAKE.md` or `UPPER_SNAKE.md`. No spaces, no dots inside filenames.
