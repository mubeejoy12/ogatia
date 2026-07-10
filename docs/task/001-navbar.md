# Task 001 — Navigation Bar

**Status:** ✅ **Done — Phase 1 scope** (2026-07-10)
**Ships in:** v0.1.0
**Location:** `src/components/layout/Navbar.tsx`

---

## Original brief

Build the Eazi Cut navigation bar.

## Requirements

| # | Requirement | Status |
|---|---|:-:|
| 1 | Sticky | ✅ |
| 2 | Transparent on hero | ✅ |
| 3 | Changes background on scroll | ✅ |
| 4 | Mobile responsive | ✅ |
| 5 | Mega menu for Shop | 🅿️ Deferred to v2 (no `/shop` route until commerce ships) |
| 6 | Collections dropdown | 🅿️ Deferred to v2 (collections page is single-page for now) |
| 7 | Search icon | 🅿️ Deferred to v2 (no product catalogue to search) |
| 8 | Cart icon with badge | 🅿️ Deferred to v2 (no cart until commerce ships) |
| 9 | User profile icon | 🅿️ Deferred to v2 (no accounts until commerce ships) |
| 10 | Login button (logged out) | 🅿️ Deferred to v2 |
| 11 | Avatar dropdown (logged in) | 🅿️ Deferred to v2 |
| 12 | Smooth animation | ✅ |
| 13 | Accessible | ✅ semantic `<nav>`, `aria-label`, focus-visible, keyboard-reachable |
| 14 | Keyboard navigation | ✅ |
| 15 | Premium luxury styling | ✅ |

## Definition of Done

> Navigation works on desktop, tablet, and mobile.

✅ Verified on iOS Safari (390 × 844), Android Chrome (360 × 800), Desktop Chrome (1440 × 900).

---

## Deferred scope justification

Requirements 5–11 all assume commerce (product catalogue, cart, accounts) which is v2 scope per `docs/management/ROADMAP.md`. Building them into the Phase 1 marketing navbar would introduce dead UI. They will be reintroduced as **Task 020 — Navbar v2 (commerce affordances)** when commerce lands.

## Implementation notes

- Sticky positioning with scroll-observed background switch.
- Mobile drawer opens with editorial (not bouncy) motion — respects `prefers-reduced-motion`.
- Uses `nav` array from `src/lib/site.ts` — no hardcoded links.
- Under 110 lines total.
