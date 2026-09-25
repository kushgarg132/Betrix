# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Casual players, mostly on phones, playing with friends and AI bots. Sessions
are social and can run long (a full hand-by-hand sit-down), so the UI has to
stay comfortable to read for extended periods, not just look good in a
first-glance screenshot.

## Product Purpose

Betrix is a play-money Texas Hold'em platform. Players sit at a table with
friends and/or AI bots (Gemini-backed, with a local random fallback) and play
real-time hands over GraphQL subscriptions. There is no real-money path —
every player at a table starts with the same fixed buy-in, and chips have no
cash value.

## Positioning

Real-time multiplayer poker against a mix of humans and AI opponents, playable
casually from a phone with a friend group, with no money, wallet, or
cashier flow anywhere in the product.

## Operating Context

- Sign-in: Google or guest (username/password is being removed as part of
  this redesign; existing password accounts are dropped, not migrated).
- Core loop: Lobby (browse/join/create tables) → Table (play hands) → Profile
  (stats, only persisted for Google-signed-in users; guest stats don't carry
  over).
- Real-time updates arrive over a GraphQL WebSocket subscription
  (`GameUpdateType`: `PLAYER_JOINED, GAME_STARTED, CARDS_DEALT,
  ROUND_STARTED, COMMUNITY_CARDS, PLAYER_ACTION, GAME_ENDED, CHAT_MESSAGE`).
  Showdown data arrives inside `GAME_ENDED` (`{ game, winners, bestHand }`),
  not a separate update type.
- Admins get an extra panel for table/game moderation.

## Capabilities and Constraints

- Frontend: React 18 (Vite), Apollo Client 4 (GraphQL), Tailwind CSS 4,
  Framer Motion, Radix UI. No new frontend npm dependencies for the redesign.
- Backend: Java 21 / Spring Boot 3.5, GraphQL-only API (no REST beyond
  actuator health), MongoDB Atlas, JWT auth.
- Mobile-first, dark theme only, target baseline viewport 375px, scaling to
  768px and 1024px+.
- Play money only: amounts must never be shown with a `$` sign; chip amounts
  are plain numbers with `K`/`M` suffixes.
- Minimum readable text size is 12px; no arbitrary sub-12px text anywhere.

## Brand Commitments

Name: Betrix. Tone: neon arcade energy — bold and game-like, but still
readable during long sessions (not a flashing, eye-straining aesthetic).
Exactly two glow accent hues are used across the whole product (cyan for
hero/primary-action/active-turn/focus, magenta for pot/wins/showdown); no
other colors glow, and glow never appears on resting/idle UI.

## Evidence on Hand

Pre-redesign screenshots of the live app (home/login, lobby, profile, and a
table) are captured in `docs/redesign/before/` at 375x812, 768x1024 and
1280x800, taken against the deployed backend
(`https://betrix.161.118.167.148.nip.io`). These are the "before" baseline
for this redesign, not final product screenshots.

## Product Principles

1. Play money, always — no `$`, no real-money/casino-gambling cues, no
   cashier/wallet framing anywhere in copy or visuals.
2. Mobile is the primary surface — design and validate the 375px layout
   first; desktop is a scale-up, not the reference.
3. Glow is a signal, not decoration — only two hues glow, and only on live
   state (active turn, focus, pot/win); everything else stays flat so the
   signal stays legible during long sessions.
4. No fabricated trust signals — no fake stats, testimonials, or numbers not
   backed by real data.
5. Legible over loud — neon arcade energy must not compromise contrast,
   minimum 12px text, or readability across a long session.

## Accessibility & Inclusion

WCAG AA (4.5:1) contrast is required for text tokens against surface
backgrounds. Touch targets at least 44px; table action buttons at least
48px tall. All motion/animation must be disabled under
`prefers-reduced-motion`.

## Baseline findings

Single-context critique + audit pass (`⚠️ DEGRADED: single-context` —
subagent dispatch is disallowed by this task's process rules) against the
15 before-screenshots in `docs/redesign/before/`.

- **Mobile home/landing (375px) is broken, not just dated.** The hero's
  copy and CTA buttons are clipped/overlapping the table-preview graphic
  (`home-375.png`) — no CTA is visible above the fold on the primary
  target device (phone). "Sign in"/"Play Now" only exist behind the
  hamburger menu on mobile. P0 for a mobile-first product.
- **Real-money-style `$` is used everywhere chip amounts appear** (lobby
  stakes "$5/10", pot "$0", stack "$10.0K", profile "Net Profit $0"). This
  directly conflicts with the stated anti-goal (no real-money/casino cues)
  and the redesign's own play-money rule (plain numbers + K/M, no `$`) —
  every one of these is a token/format change, not a one-off.
- **Empty profile stats read as fabricated data.** A brand-new guest sees
  "0 Hands Played / 0% Win Rate / $0 Net Profit" styled identically to real
  stats, rather than an honest empty/first-run state — conflicts with the
  "no fake stats" anti-goal.
- **Current visual language is muted casino/vintage-parlor, not arcade.**
  Serif display type (Playfair-style), gold/amber accents, and a felt-green
  table read as a traditional card-room, the opposite pole from the neon
  cyan/magenta direction — confirms the spec's plan to replace the token
  system wholesale rather than retint it.
- **No glow/live-state signal exists today.** All actionable elements
  (buttons, the "Need 1 more player" pill, active seat) are flat gold; there
  is no visual distinction between resting and live UI. The redesign's
  two-accent glow system is a new signal to introduce, not a retint of an
  existing one.
- **Tablet/desktop table layout wastes width and misaligns.** At 768px the
  oval table is small and off-center between the two fixed side panels with
  large dead black gutters (`table-768.png`); the table doesn't grow to
  fill the viewport the way the info/chat panels do.
- **Lobby empty state is a solid baseline to reuse.** Copy ("Be the first
  to deal…") and layout hold up cleanly at all three widths — worth
  carrying forward rather than rewriting from scratch.

Known implementation gap hit while capturing the table screenshot (not a
design finding, flagged for later tasks): in `GameLobby.jsx`, after
`createGame` succeeds (confirmed 200 response with a valid `id` over the
network), the lobby's own `navigate(`/game/${id}`)` does not fire and the
UI stays on `/lobby` despite a "Table created!" toast. The table screenshot
was captured by navigating directly to `/game/<id>` using the id read off
the network response instead. Root cause wasn't investigated further since
this task changes no source code.
