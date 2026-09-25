# Betrix neon redesign + Google sign-in: design

Date: 2026-09-25. Branch: `redesign` (cut from `backend-hardening`). Status: approved in brainstorming, pending spec review.

## Decisions

| Topic | Decision |
|---|---|
| Visual direction | Neon / game-UI |
| Viewport | Mobile-first (375px baseline), scales to desktop |
| Theme | Dark only |
| Delivery | `redesign` branch, Firebase preview channel, one merge to `master` at the end |
| Auth | Google sign-in + guest only. Username/password removed; existing password accounts are dropped (not migrated) |
| Approach | Tokens first, restyle existing components in place, rebuild only the table screen. No new frontend dependencies |
| Table UX additions | Turn countdown ring, connection indicator, mobile chat + leave, animated showdown reveal |

Money model is unchanged: play money per table (see backend-hardening).

## 1. Design tokens

Replace the `@theme` block in `frontend/src/index.css`. Existing token names are kept where the role still fits (`background`, `surface*`, `text*`, `border*`, `danger`, `success`, `warning`) so most components restyle through values alone. `felt*`, `rim*` and `gold*` are removed and their usages migrated.

- Surfaces: `background #07070f`, `surface #0e0e1a`, `surface-elevated #161628`, `surface-overlay #1f1f38`. Borders are white at 8-14% alpha.
- Accents (exactly two glowing hues):
  - `neon-cyan #22e4ff`: hero seat, primary action, active turn, focus rings.
  - `neon-magenta #ff2bd6`: pot, wins, showdown.
- Semantic (never glow): `danger #ff4d6d` (fold, offline), `success #3dffa0` (check/call, online), `warning #ffb020` (turn timer at 5s or less).
- Glow: `--glow-sm`, `--glow-md`, `--glow-lg` box-shadow tokens built with `color-mix()` from the accents. Glow is applied only to live state (active seat, focused control, winner). Resting UI is flat.
- Table: `table-floor` (dark radial gradient), `table-rail` (2px cyan line with outer glow), `chip-1` to `chip-4` denominations.
- Type: Space Grotesk (display: headings, pot, chip counts), Inter (body), JetBrains Mono with `font-variant-numeric: tabular-nums` (chip amounts, timers). Playfair Display is removed. Scale 12/14/16/20/24/32/48px; nothing below 12px. All `text-[8-11px]` arbitrary sizes and hardcoded hex values in components are replaced with tokens.
- Text: `text #f2f3ff`, `text-muted #a3a6c8`, `text-dim #7c80a6`. All must meet WCAG AA (4.5:1) on `surface`.
- Motion: `--ease-out-expo`; durations 120/200/400ms. All pulses, glows-in-motion and slides are disabled under `prefers-reduced-motion`.

Final hex values may be adjusted during the ui-ux-pro-max design-system step, but the roles above are fixed.

## 2. Screens (mobile-first)

- Shell: mobile has a slim top bar (logo, chip count, avatar menu) and a bottom tab bar (Lobby / Profile / Admin for admins). At 1024px and wider the tab bar folds into the top bar. The table route hides both.
- Home is the sign-in screen: wordmark with a slow neon flicker, one line of copy, two stacked buttons: "Continue with Google" (Google's official rendered button, dark variant) and "Play as guest" (cyan outline). Signed-in users are redirected to the Lobby. `Login.jsx` and `Register.jsx` are deleted and their routes removed.
- Lobby: single-column list of table rows (name, blinds, seat-fill dots, status chip, one action: Join / Rejoin when `isYourGame` / Full disabled). The user's own games are pinned first. A sticky "Create table" button opens the existing `BottomSheet`. Two columns at 768px and wider.
- Profile: Google avatar and name, stat tiles in a 2-column grid, recent games. Guests see a "Sign in with Google to keep your stats" prompt. Guest stats do not carry over.
- Admin: the Lobby list pattern with admin actions; functional styling only.
- NotFound: "folded" empty state linking to the Lobby.

## 3. Table screen

`pages/PokerTable.jsx` (617 lines) becomes a thin layout page. Logic moves into hooks; UI into focused components under `components/poker/`.

Hooks:
- `useGame(gameId)` (existing file, expanded): game query, `GameUpdate` subscription, mutations (bet/check/fold/leave/sitOut/sitIn/start/addBot), hero seat, `isMyTurn`, chat messages, `lastShowdown` (from `SHOWDOWN` updates, including `bestHand`), and the action deadline.
- `useConnectionStatus()` (new): subscribes to the graphql-ws client's `connecting` / `connected` / `closed` events (wired in `api/apolloClient.js`) and returns `online | reconnecting | offline`.

Components (portrait phone layout: top bar, scene, action bar):
- `TableTopBar`: leave button (confirm sheet, calls `leaveGame`), table name + blinds, `ConnectionPill`, chat button with unread dot.
- `TableScene`: vertical oval on mobile, horizontal at 1024px and wider (`OvalTable` restyled). Seat positions come from a pure function `seatLayout(playerCount, heroIndex)` that always places the hero bottom-center (replaces the hardcoded `SEAT_POSITIONS[0]`). Community cards and `PotDisplay` in the center.
- `PlayerSeat`: avatar, name, mono chip count, current bet chip, status (folded 40% opacity, all-in magenta tag, sitting out grey).
- `TurnRing`: SVG ring around the acting seat, progress from `currentPlayerActionDeadline` and `playerActionTimeoutSeconds`. Cyan; warning amber at 5s or less. Calls `navigator.vibrate` once when the hero's turn starts, if supported.
- `ActionBar` (restyled `BettingControls`): pinned bottom; Fold / Check-Call / Raise at 48px minimum height. Raise opens a slider with half-pot, pot and all-in presets. Off-turn: Sit out / Sit in; pre-hand: Start hand, Add bot. Disabled while not the hero's turn or while connection is not `online`.
- `ChatDrawer`: existing `BottomSheet` on mobile, docked right panel at 1024px and wider. Tabs: Chat, Hands (existing rankings panel). Chat items keyed by message id.
- `ShowdownReveal` (replaces `WinnerOverlay`): winning seats glow magenta, the 5 `bestHand` cards lift and others dim, hand-name banner (e.g. "Full House"), chips animate to the winner. About 3s; tap to skip. Incoming next-hand state is queued until the reveal ends.
- `ConnectionPill`: hidden when online; amber "Reconnecting…"; red "Offline, actions paused".

The current left sidebar is removed; its controls move to `TableTopBar` and `ActionBar`.

## 4. Google sign-in (backend)

- Dependency: `org.springframework.security:spring-security-oauth2-jose` (provides `NimbusJwtDecoder`).
- Schema: add `googleLogin(idToken: String!): AuthPayload!`. Remove `login`, `register`, `LoginInput`, `RegisterInput`.
- Verification: `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` with validators for issuer (`accounts.google.com` or `https://accounts.google.com`), audience equal to `GOOGLE_CLIENT_ID`, expiry, and `email_verified == true`. Any failure returns an auth error, never a partial login.
- User model: add `googleSub` (unique index), `email`, `avatarUrl`. Remove `password` and the password encoder. On sign-in: find by `googleSub`, else create; refresh name, email, avatar each time. Role `ADMIN` if the email is in `APP_ADMIN_EMAILS` (comma-separated), else `USER`. Then issue the existing app JWT; nothing downstream (HTTP or WebSocket auth) changes.
- Rate limiting: the existing auth-mutation rate limit also covers `googleLogin`.
- Config: `GOOGLE_CLIENT_ID` and `APP_ADMIN_EMAILS` in backend env (`betrix.env` in the runner dir for prod); `VITE_GOOGLE_CLIENT_ID` in the frontend build (Firebase workflow). Startup fails fast if `GOOGLE_CLIENT_ID` is blank, matching the JWT secret validation; the test profile sets a dummy value so `contextLoads` still runs without real credentials.
- Frontend: load Google Identity Services script (`https://accounts.google.com/gsi/client`) on Home only, render its button, send the credential to `googleLogin`, store the returned JWT the same way guest login does.
- Manual prerequisite (Kush): create an OAuth 2.0 Web client in Google Cloud Console. Authorized JavaScript origins: `https://betrix-b3c24.web.app`, `https://betrix-b3c24.firebaseapp.com`, `http://localhost:3000` (the Vite dev port in `vite.config.js`), and the preview channel URL once created.

## 5. States, errors, accessibility

- Loading: layout-shaped skeletons (lobby rows, table seats); no full-screen spinners.
- Empty: lobby "No tables yet" with Create; empty chat hint.
- Errors: GraphQL/mutation errors surface as `sonner` toasts in the danger color. A rejected action re-enables the action bar. Google sign-in failure shows an inline message with retry on Home; guest login stays available.
- Session: expired JWT or unauthenticated response clears the token and redirects to Home with a "Session expired" toast.
- Table missing or player removed: redirect to Lobby with a toast.
- Accessibility: cyan focus rings on every interactive element; `aria-live="polite"` announcements for "Your turn" and the showdown result; all actions keyboard-reachable, with F / C / R shortcuts on desktop; touch targets at least 44px (action buttons 48px); reduced motion respected.

## 6. Workflow and build order

Worktree: `/home/ubuntu/projects/Betrix-wt-redesign`, branch `redesign`, pushed to origin as backup.

1. `impeccable` shape step writes `PRODUCT.md`; `impeccable` critique + audit on current screens; Playwright "before" screenshots at 375/768/1280.
2. Tokens and fonts (`index.css`, `index.html`); restyle `components/ui/*`.
3. Google sign-in backend; Home sign-in screen; delete Login/Register.
4. Shell, Lobby, Profile, Admin, NotFound.
5. Table: `useGame` refactor, then TableScene/PlayerSeat, ActionBar, TurnRing, ConnectionPill, ChatDrawer, ShowdownReveal.
6. `impeccable` polish + audit per screen; `impeccable-finish-reviewer` against this spec; `impeccable-documenter` writes `DESIGN.md`.

Each step leaves the app building and working.

## 7. Testing

- Backend: `googleLogin` tests with a stubbed `JwtDecoder`: valid token creates user; second sign-in updates name/avatar; wrong audience rejected; unverified email rejected; admin email gets `ADMIN`. The full existing suite stays green.
- Frontend (vitest + Testing Library):
  - `seatLayout` puts the hero bottom-center for every player count 2-9 and every hero index.
  - TurnRing progress and the amber threshold computed from deadline and timeout.
  - `useConnectionStatus` event-to-state mapping and `ConnectionPill` rendering.
  - `ActionBar` disabled when offline or not the hero's turn.
  - `ShowdownReveal` highlights exactly the `bestHand` cards.
  - Home renders both sign-in options; Login/Register routes are gone.
  - Existing 13 tests updated, not deleted.
- `npm run build` after every step (vitest does not type/build-check).
- Visual: Playwright screenshots of every screen at 375/768/1280 after each step; one real guest + bot hand through showdown on the preview.

## 8. Release

- Preview: `firebase hosting:channel:deploy redesign`, pointed at a separate backend run from the `redesign` worktree on a spare host port (not the live container). The preview is served over HTTPS, so that backend gets its own Nginx site + Let's Encrypt cert (`betrix-preview.161.118.167.148.nip.io`, WebSocket location for `/graphql` as in `betrix.conf`), and its CORS allow-list includes the preview channel origin. Removed after merge.
- Merge to `master` only after Kush (a) creates the Google OAuth client and provides the client ID, (b) approves the preview. Kush chose not to rotate the Atlas password and Gemini key that are in public git history (2026-09-25); recommended mitigations are an Atlas IP allowlist limited to the VM and API restrictions plus a quota on the Gemini key. One push then deploys backend and frontend together via the existing pipelines.

## Out of scope

Light theme; moving the token to an httpOnly cookie; pagination; `GameUpdate.payload` typing; carrying guest stats into a Google account; hand history beyond the existing rankings tab; migrating password accounts.
