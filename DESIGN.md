---
name: Betrix
description: Play-money neon poker — a dark arcade table where only two colors ever glow.
colors:
  background: "#07070f"
  surface: "#0e0e1a"
  surface-elevated: "#161628"
  surface-overlay: "#1f1f38"
  border: "rgb(255 255 255 / 0.08)"
  border-strong: "rgb(255 255 255 / 0.14)"
  text: "#f2f3ff"
  text-muted: "#a3a6c8"
  text-dim: "#8286ac"
  text-inverse: "#07070f"
  neon-cyan: "#22e4ff"
  neon-magenta: "#ff2bd6"
  danger: "#ff4d6d"
  success: "#3dffa0"
  warning: "#ffb020"
  table-floor: "#0b0b1c"
  table-rail: "#22e4ff"
  chip-1: "#3dffa0"
  chip-2: "#22e4ff"
  chip-3: "#ff2bd6"
  chip-4: "#ffb020"
typography:
  display:
    fontFamily: "'Space Grotesk', system-ui, sans-serif"
    fontWeight: 700
    letterSpacing: "normal"
  body:
    fontFamily: "'Inter', system-ui, -apple-system, sans-serif"
    fontWeight: 400
    lineHeight: 1.4
  mono:
    fontFamily: "'JetBrains Mono', ui-monospace, monospace"
    fontFeature: "tabular-nums"
rounded:
  xs: "0.25rem"
  sm: "0.375rem"
  md: "0.5rem"
  lg: "0.625rem"
  xl: "0.75rem"
  2xl: "1rem"
  3xl: "1.5rem"
  full: "9999px"
spacing:
  touch-min: "44px"
  action-min: "48px"
components:
  button-default:
    backgroundColor: "{colors.neon-cyan}"
    textColor: "{colors.text-inverse}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "44px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.neon-cyan}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "44px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.text-muted}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "44px"
  badge-default:
    backgroundColor: "{colors.neon-cyan}"
    textColor: "{colors.neon-cyan}"
    rounded: "{rounded.full}"
    padding: "2px 10px"
  input-default:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.md}"
    height: "44px"
    padding: "0 12px"
---

# Design System: Betrix

## Overview

**Creative North Star: "The Blackout Arcade Table"**

Betrix is a single dark room lit by exactly two neon signs. Everything is black-on-black by default — background, surfaces, borders — and stays that way until something is actually happening: a hand is live, it's your turn, a pot is being won. Only then does cyan or magenta switch on, and the glow itself is the entire vocabulary of "this matters right now." The rest of the interface (lobby rows, profile stats, form fields) is flat, high-contrast, and calm, because the product's own users play long sit-down sessions on a phone and the arcade energy has to survive an hour of reading, not just a first screenshot.

The previous direction (serif display type, gold/amber accents, felt-green table) was fully replaced, not retinted — Playfair Display and every felt/rim/gold token are gone from the codebase. The redesign is deliberately restrained about *where* neon appears: it is a signal system layered on a dark-mode utility app, not a wall-to-wall glow effect.

**Key Characteristics:**
- Pure black-adjacent surfaces (`#07070f` → `#1f1f38`) with no warm tones anywhere
- Exactly two glow hues, cyan and magenta, gated strictly to live state
- Three-tier semantic color (danger/success/warning) that is loud in hue but never glows, keeping it visually subordinate to the two real signals
- Three-font system with a hard division of labor: display for identity/scale, body for reading, tabular mono for anything numeric that changes in real time (chips, timers)
- Soft, continuous corner radii everywhere (no sharp corners, no neobrutalist hard-offset shadows) — the game-UI mood comes from glow and type, not geometry

## Colors

Near-black surfaces stepped in four tones, two glowing accents, three flat semantics, and a four-slot chip-denomination ramp that reuses the accent/semantic hues rather than inventing new ones.

### Primary
- **Neon Cyan** (`#22e4ff`): the hero/self signal — hero seat ring, primary buttons, active-turn ring, focus rings on every interactive element. The single most-used color in the system; anywhere the product needs to say "this is you, or this is the primary action," it's cyan.
- **Neon Magenta** (`#ff2bd6`): the outcome signal — live pot amount, showdown reveal banner, winning cards/seat glow, win state. Never used for anything ongoing or neutral; magenta always means "a hand just resolved" or "money is actively at stake."

### Secondary (semantic, never glow)
- **Danger Red** (`#ff4d6d`): fold, offline/error states, destructive actions.
- **Success Green** (`#3dffa0`): check/call, online status, the lowest chip denomination.
- **Warning Amber** (`#ffb020`): turn timer under 5 seconds, "waiting for players," reconnecting.

### Neutral
- **Void Black** (`#07070f`, `background`): the base app background.
- **Ink Surface** (`#0e0e1a`, `surface`): cards, inputs, table rail interior.
- **Raised Surface** (`#161628`, `surface-elevated`): dialogs, sheets, elevated panels.
- **Overlay Surface** (`#1f1f38`, `surface-overlay`): the highest layer — showdown banner, dropdown menus.
- **Hairline Border** (`rgb(255 255 255 / 0.08)`) / **Strong Border** (`rgb(255 255 255 / 0.14)`): the only two border weights in the system; strength is opacity, never a color change.
- **Primary Text** (`#f2f3ff`), **Muted Text** (`#a3a6c8`), **Dim Text** (`#8286ac`): a three-step text ramp, all confirmed against dark surfaces at WCAG AA.

### Named Rules
**The Two-Hue Signal Rule.** Cyan and magenta are the only colors in the product that ever glow (`box-shadow` built from `--glow-*` tokens), and glow is applied only to live state — an active turn, a focused control, a live pot, a win. A resting cyan button or an idle magenta label never carries the glow shadow; color alone marks role, glow alone marks "this is happening now." (The pot pill is the concrete test: it shows magenta text at rest and only gains the glow shadow once the pot is actually live and non-zero.)

**The No-Dollar-Sign Rule.** Every chip amount in the product is a plain number with a `K`/`M` suffix rendered in tabular mono, never a currency symbol, anywhere — lobby stakes, pot, stack, profile stats. This is a durable product constraint (play money only, no cash-value framing), not a style preference, and it should never be relaxed for a new screen.

## Typography

**Display Font:** Space Grotesk (with system-ui, sans-serif fallback)
**Body Font:** Inter (with system-ui, -apple-system, sans-serif fallback)
**Label/Mono Font:** JetBrains Mono (with ui-monospace, monospace fallback), always `font-variant-numeric: tabular-nums` when set on a number

**Character:** A geometric, slightly technical display face paired with a neutral, highly-legible body face — confident and game-like at large sizes, unobtrusive at reading sizes. Mono is reserved entirely for numbers that need to not jitter as they update (chip stacks, countdown timers).

### Hierarchy
- **Display** (Space Grotesk, bold, large/clamp sizes): wordmark, page titles ("Lobby"), showdown hand-name banner. On the Home wordmark only, a slow `neonFlicker` keyframe (6s loop, brief dips to ~55-70% opacity) is layered on top — a one-off hero treatment, not a general text-motion rule (see Do's and Don'ts).
- **Body** (Inter, regular/medium, 14-16px): all UI copy, labels, descriptions, table names.
- **Numeric/Mono** (JetBrains Mono, tabular-nums, 12-24px depending on context): chip stacks, pot amount, bet sliders, turn countdown. Never used for prose.
- **Scale floor**: nothing below 12px anywhere in the product; this is an explicit accessibility floor, not an incidental minimum.

### Named Rules
**The Tabular-Numbers Rule.** Any number that can change while the user is looking at it (chip count, pot, timer) is set in JetBrains Mono with tabular-nums, so digit width never shifts and the layout doesn't jitter during live play.

## Layout

Mobile-first, 375px baseline scaling to 768px and 1280px+. Lobby and Profile use a single-column card list on mobile that becomes a two-column grid at 768px and wider (confirmed in `lobby-1280.png`). The table screen is the one layout that changes shape rather than just adding columns: a vertical oval with a bottom-pinned action bar on mobile (portrait), a horizontal oval with side panels at 1024px and wider (landscape/desktop). Top bar + content + bottom action/tab bar is the standing shell shape everywhere except the table route, which hides both bars in favor of its own `TableTopBar`/`ActionBar`.

Touch targets are at least 44px; table action buttons (Fold/Check/Raise) are at least 48px tall, confirmed larger than the general minimum because they're the highest-frequency interaction in the product.

## Elevation & Depth

Betrix does not use drop shadows for hierarchy. Depth is tonal: four flat surface steps (`background` → `surface` → `surface-elevated` → `surface-overlay`) stack from base app to top-most overlay, each one a lighter near-black, with a hairline or strong border (opacity-only) marking the edge. The only shadow vocabulary in the system is the glow set, and glow is a state signal, not a depth cue — a glowing element isn't "higher," it's "live."

### Shadow Vocabulary
- **Glow Small** (`0 0 6px color-mix(in oklab, var(--color-neon-cyan) 55%, transparent)`): focus rings.
- **Glow Medium** (`0 0 14px color-mix(in oklab, var(--color-neon-cyan) 50%, transparent)`): primary button hover, table rail.
- **Glow Large** (`0 0 28px color-mix(in oklab, var(--color-neon-cyan) 45%, transparent)`): table floor ambient glow.
- **Glow Magenta Medium** (`0 0 16px color-mix(in oklab, var(--color-neon-magenta) 55%, transparent)`): live pot, showdown banner, winning seat/cards.

### Named Rules
**The Flat-By-Default Rule.** Surfaces are flat at rest; tonal layering (not shadow) carries hierarchy. The glow shadows exist only as a response to live state (active turn, focus, win) — never as an ambient "card lift" effect.

## Shapes

Soft, continuous corner radii throughout — no sharp corners and no neobrutalist hard-offset shadows anywhere in the system. The radius scale runs from `--radius-xs` (4px, small chips/pips) through `--radius` (8px, default control) up to `--radius-2xl` (24px, dialogs/sheets) and `--radius-full` (pills, badges, chips, avatars). Cards, dialogs, and buttons all round; the poker table itself and its chips are the one fully circular/elliptical geometry in the product, appropriately, since they're the literal table and chips.

## Components

### Buttons
- **Shape:** rounded to the default radius (8px, `var(--radius)`), pill-shaped only for icon-only sizes at the same radius.
- **Default (primary):** solid neon-cyan background, inverse-black text, semibold; gains the medium glow shadow on hover. This is the one button variant meant to visually dominate a screen.
- **Outline:** cyan border and text on a transparent fill — the standard secondary action (e.g. "Play as guest," "Join").
- **Ghost:** muted text, no border, surface-elevated background on hover — low-emphasis actions.
- **Danger / Success:** 15%-alpha tinted background with a matching 40%-alpha border and full-strength text, used for Fold and Check/Call on the table action bar. These never glow, consistent with semantic colors staying flat.
- **Surface:** neutral elevated-surface button for tertiary choices (e.g. raise presets).
- Sizes range 36-56px tall; the table's Fold/Check/Raise row uses the large/xl sizes to clear the 48px action-button floor.

### Badges / Chips (status)
- **Style:** full-pill radius, 15%-alpha tinted background matching a 40%-alpha border of the same hue, small semibold uppercase-weight label (not literally uppercase text, but bold and compact).
- **State:** `default`/`info`/`active` all resolve to cyan; `success`/`waiting`/`warning` map to their semantic color; `completed` is neutral surface-elevated. Status pills (e.g. "Waiting for players," "Pre-Flop") read as informational, never urgent, unless amber/warning.

### Poker Chip (signature component)
Circular token, 4 sizes (`sm`/`md`/`lg`), with a dashed inner ring and a bold tabular-mono amount label. Color is a denomination ramp reusing existing system hues rather than a separate chip palette: green (`chip-1`, ≥0), cyan (`chip-2`, ≥25), magenta (`chip-3`, ≥100), amber (`chip-4`, ≥500). New-chip entries animate in with a spring drop (translateY + scale) via Framer Motion.

### Poker Card (signature component)
Rounded-small rectangle (`--radius-sm`), white face with red/black-on-inverse pip text depending on suit; face-down cards show a cyan diagonal-stripe pattern instead of a solid back, keeping even the card back on-brand. A `highlighted` state (winning cards at showdown) lifts the card 2 units, adds a magenta ring, and the magenta glow shadow — the same glow-as-live-state vocabulary as everywhere else, not a card-specific effect.

### Turn Ring (signature component)
An SVG ring stroked around the acting seat, animating its `stroke-dashoffset` down as the turn timer elapses. Cyan under normal time remaining, switching to amber (with the ring's glow dropped, per the urgency reading — flat when urgent, not brighter) inside the last 5 seconds. Reflects the pattern that amber deliberately never glows even under time pressure, keeping magenta and cyan the only two hues that ever do.

### Connection Pill (signature component)
Hidden entirely when the connection is healthy — only renders for `reconnecting` (amber, spinning loader icon) or `offline` (red, wifi-off icon), each with `role="status" aria-live="polite"`. A concrete instance of the flat-semantics rule: connection state is urgent information, but amber/red carry that urgency through color and an icon, never through glow.

### Showdown Reveal (signature component)
A tappable, dismissible banner centered over the table (not a modal) on the overlay surface with a magenta border and the magenta glow, showing the winner sentence in body text and the hand name (e.g. "Full House") in large glowing-magenta display type. Auto-dismisses after 3 seconds or on tap; announced via a visually-hidden `aria-live="polite"` region for the same content.

### Inputs / Fields
- **Style:** surface background, strong border, default radius, 44px tall.
- **Focus:** border switches to neon-cyan; no separate ring shadow on the input itself (the ring/glow pattern is reserved for the global `:focus-visible` outline + small glow, applied uniformly across all interactive elements, not re-implemented per component).

### Dialog / Sheet
- Surface-elevated background, strong border, extra-large radius (`--radius-xl`, 12px), opens with a scale+fade+slide entrance (200ms). Close affordance is a small ghost icon button in the corner.

## Do's and Don'ts

### Do:
- **Do** gate glow strictly to live state (active turn, focus, live pot, win) — never apply a `--glow-*` shadow to resting UI.
- **Do** render every chip amount as a plain number with K/M suffix in tabular mono; never a `$` sign.
- **Do** keep danger/success/warning flat (no glow) so cyan and magenta remain the only two colors that signal "look here."
- **Do** use the four-step tonal surface ramp (`background` → `surface` → `surface-elevated` → `surface-overlay`) for hierarchy instead of shadows.
- **Do** keep table action buttons at 48px minimum height and every other interactive target at 44px minimum.

### Don't:
- **Don't** add a third glowing hue. The system's legibility over long sessions depends on glow staying a two-color, state-only signal.
- **Don't** reach for a hard-offset/neobrutalist shadow anywhere in this system; the shape language is soft radii plus tonal layering, and the only shadow vocabulary that exists is the glow set.
- **Don't** generalize the Home wordmark's `neonFlicker` animation into a system-wide text-motion pattern — it's a single deliberate hero flourish used exactly once, not a reusable token (see the "not canonized" note below).
- **Don't** introduce felt, gold, rim, or serif-display tokens; that identity was fully and deliberately removed by this redesign, not merely retinted.
