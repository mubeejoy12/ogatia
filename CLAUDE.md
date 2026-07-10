# CLAUDE.md — Root AI Entry Point

Claude Code and other AI assistants: **read this file first, every session.**

---

## Reading order (mandatory, in this order)

1. `/MASTER_CONTEXT.md` — one-file project summary
2. `/CLAUDE.md` — this file (root rules + reading map)
3. `/docs/ai/CLAUDE.md` — extended AI operating instructions
4. `/docs/README.md` — documentation index
5. Whichever spec doc governs the task you're about to do

Do not skip step 1. Do not paraphrase from memory — the docs are the source of truth.

---

## What this project is

Eazi Cut is a **luxury African menswear brand** — bespoke tailoring, ready-to-wear,
and wedding commissions. The digital platform is a **luxury fashion house on the web**.

The customer is spending ₦500,000 or more on a single commission. Every page must
earn that trust. Nothing about this site should ever feel like a startup, a SaaS
dashboard, or a generic ecommerce theme.

---

## What this project is NOT

- ❌ A SaaS application
- ❌ A dashboard, admin panel, or B2B tool
- ❌ A startup landing page
- ❌ A generic ecommerce theme
- ❌ A "modern app" with purple gradients, feature grids, or "get started" heroes
- ❌ A place for cookie-cutter tailwind starter aesthetics

If you cannot picture the page next to Tom Ford, Dior Homme, or Ozwald Boateng
without wincing — you have the wrong reference.

---

## Non-negotiable rules

1. **Colours:** ink `#0A0A0A`, ivory `#F5F1EA`, gold `#B8893E` (+ soft `#D4A85C`, deep `#8C6628`). Nothing else. **If `docs/02_BRAND_GUIDELINES.md` says otherwise (Cormorant, `#111111`, `#C8A045`), the code tokens win until the doc is reconciled.**
2. **Typography:** Playfair Display (display), Inter (body). Nothing else. Same reconciliation note as above.
3. **Images:** every image resolves through `src/lib/assets.ts`. Never hardcode a URL.
4. **Motion:** editorial easing only (`cubic-bezier(0.22, 1, 0.36, 1)`). Respect `prefers-reduced-motion`.
5. **Data:** collections, lookbook, testimonials, process live in `src/lib/data/*`. Never inline.
6. **Architecture:** feature modules in `src/features/`, hooks in `src/hooks/`, network in `src/services/`, types in `src/types/`.
7. **Explain before you build.** For anything non-trivial, state the plan first.
8. **Never commit** without an explicit ask.
9. **Never** use `--force`, `--no-verify`, `--legacy-peer-deps` without the user requesting it.
10. **Never invent brand facts.** If the spec doesn't say it, ask.
11. **Launch focus:** the project is optimising for an end-of-July 2026 marketing-site launch. Do not add scope. Full ecommerce and bespoke flows are v2/v3 and out of scope until the marketing site is live.

---

## Where things live

| I need to… | Look here |
|---|---|
| Understand the project fast | `/MASTER_CONTEXT.md` |
| Find any doc | `/docs/README.md` |
| Check brand voice / colour / type | `/docs/business/02_BRAND_GUIDELINES.md` |
| Check product requirements | `/docs/product/11_FEATURE_REQUIREMENTS.md` |
| Check system architecture | `/docs/engineering/13_SYSTEM_ARCHITECTURE.md` |
| Check current status | `/docs/management/PROJECT_STATUS.md` |
| See the roadmap | `/docs/management/ROADMAP.md` |
| See what's in the backlog | `/docs/management/BACKLOG.md` |
| Read the extended AI rules | `/docs/ai/CLAUDE.md` |
| Find image placeholders | `/src/lib/assets.ts` |
| Find site copy / config | `/src/lib/site.ts` |

---

## Working style expected

- **Terse, honest, no filler.** Say what you're doing and why.
- **Audit before you build.** If asked to build something, first check what already exists.
- **Prefer editing existing files** over creating new ones.
- **Never create documentation** unless asked. This repo has enough docs.
- **Never create emojis in code or docs** unless asked.
- **When you finish, verify.** Read what you wrote back to yourself before claiming done.

---

## When you break a rule

Stop, tell the user which rule and why the situation required it, and get
consent before continuing. Do not silently proceed.
