# Betrix Neon Redesign + Google Sign-in Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace username/password auth with Google sign-in + guest, and restyle the whole frontend into a mobile-first neon/game-UI look with a rebuilt table screen (turn ring, connection pill, mobile chat/leave, showdown reveal).

**Architecture:** Backend adds one `googleLogin` mutation that verifies a Google ID token with Spring Security's `NimbusJwtDecoder` and then issues the existing app JWT; password auth is deleted. Frontend swaps the `@theme` tokens, restyles existing components in place, and splits the 617-line `PokerTable.jsx` into pure, tested logic (`gameReducer`, `seatLayout`, `turnProgress`, `showdown`) plus small presentational components.

**Tech Stack:** Spring Boot 3.5 / Gradle / GraphQL / MongoDB; Vite 7, React 18, Tailwind 4, Apollo Client 4.1.6, graphql-ws 6.0.8, framer-motion, Radix, vitest.

**Spec:** `docs/superpowers/specs/2026-09-25-neon-redesign-design.md` (read it before starting any task).

## Global Constraints

- Work only in worktree `/home/ubuntu/projects/Betrix-wt-redesign`, branch `redesign`. Never push to `master` (it auto-deploys prod). `git push origin redesign` after each task is fine.
- Commits: identity is already configured. End every commit message with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Backend commands run from `backend/`: `./gradlew test` (full suite), `./gradlew test --tests '<FQCN>'` (one class). The build is Gradle, not Maven.
- Frontend commands run from `frontend/`: `npx vitest run <path>` and **also `npm run build`** after every frontend task (vitest does not type/build-check). Dev server port is **3000** (`vite.config.js`), not 5173.
- No new frontend npm dependencies. Only new backend dependency: `org.springframework.security:spring-security-oauth2-jose` (version managed by Spring Boot).
- Dark theme only. Mobile-first: style the 375px layout first, then `md:` (768px) and `lg:` (1024px) overrides.
- Colors come from tokens only. No hardcoded hex in components; no `text-[8px]`..`text-[11px]` arbitrary sizes (minimum text size 12px = `text-xs`).
- Exactly two glow accents: `neon-cyan` (hero, primary action, active turn, focus) and `neon-magenta` (pot, wins, showdown). Semantic `danger/success/warning` never glow. Glow only on live state.
- Play money: never show `$`. Chip amounts are plain numbers with `K`/`M` suffixes.
- Every animation must be disabled under `prefers-reduced-motion`.
- Touch targets at least 44px; table action buttons at least 48px tall.
- Apollo Client 4: `useQuery` has **no** `onCompleted`/`onError` options. React to `data`/`error` in `useEffect`.
- Real-time update types sent by the backend (`GameUpdateType`): `PLAYER_JOINED, GAME_STARTED, CARDS_DEALT, ROUND_STARTED, COMMUNITY_CARDS, PLAYER_ACTION, GAME_ENDED, CHAT_MESSAGE`. There is no `SHOWDOWN` update: showdown data arrives in `GAME_ENDED` as `{ game, winners, bestHand }`, with `bestHand` null when the hand ended without a showdown.
- Skills to invoke (Skill tool) where a task says so: `impeccable:impeccable`, `ui-ux-pro-max:design-system`, `example-skills:webapp-testing`.

---

## File map

Backend (`backend/src/main/java/com/example/backend/`):
- Create `security/GoogleIdTokenVerifier.java`: verifies a Google ID token, returns `GoogleIdentity`.
- Modify `service/UserService.java`: replace `createUser` with `signInWithGoogle`.
- Modify `entity/User.java`: drop `password`, add `googleSub` (unique sparse index), `avatarUrl`.
- Modify `repository/UserRepository.java`: add `findByGoogleSub`, drop the `existsBy*` methods.
- Modify `resolver/MutationResolver.java`: add `googleLogin`, delete `login`/`register`.
- Modify `config/SecurityConfig.java`: delete `PasswordEncoder` and `AuthenticationManager` beans.
- Delete `config/DataSeeder.java`, `model/LoginInput.java`, `model/RegisterInput.java`.
- Modify `resources/graphql/schema.graphqls`, `resources/application.properties`, `resources/application-local.properties`, `resources/META-INF/additional-spring-configuration-metadata.json`, `test/resources/application-test.properties`.

Frontend (`frontend/src/`):
- `index.css`, `../index.html`: tokens and fonts.
- `lib/utils.js`: `formatChips` without `$`.
- `components/ui/*`: restyled primitives.
- `context/AuthContext.jsx`: Apollo 4 fix, session expiry.
- `hooks/useAuth.js`, `graphql/mutations.js`, `graphql/queries.js`: Google login, avatar.
- Create `components/auth/GoogleSignInButton.jsx`; rewrite `pages/Home.jsx`; delete `pages/Login.jsx`, `pages/Register.jsx`.
- `components/layout/Navbar.jsx` becomes `TopBar.jsx` + `BottomTabBar.jsx`; `App.jsx` routes.
- `pages/GameLobby.jsx`, `components/lobby/GameCard.jsx` (row layout), `pages/Profile.jsx`, `pages/AdminPanel.jsx`, `pages/NotFound.jsx`.
- `api/apolloClient.js`: export `wsClient`; create `hooks/useConnectionStatus.js`, `components/poker/ConnectionPill.jsx`.
- Create `lib/gameReducer.js`; rewrite `hooks/useGame.js`.
- Create `lib/seatLayout.js`, `lib/turnProgress.js`, `lib/showdown.js`.
- Create `components/poker/TurnRing.jsx`, `TableTopBar.jsx`, `ChatDrawer.jsx`, `ShowdownReveal.jsx`, `ActionBar.jsx` (replaces `BettingControls.jsx`); modify `OvalTable.jsx`, `PlayerSeat.jsx`, `PokerCard.jsx`; delete `WinnerOverlay.jsx`, `BettingControls.jsx`; rewrite `pages/PokerTable.jsx`.
- Docs: `PRODUCT.md`, `DESIGN.md` at repo root.

---

### Task 1: Design baseline (PRODUCT.md + before screenshots)

**Files:**
- Create: `PRODUCT.md`
- Create: `docs/redesign/before/*.png` (screenshots)

**Interfaces:** Produces `PRODUCT.md`, which later tasks and the Task 16 finish review read.

- [ ] **Step 1: Invoke `impeccable:impeccable` and run its shape/document step to produce `PRODUCT.md`.** Feed it these facts: product = play-money Texas Hold'em with friends and AI bots; users = casual players, mostly on phones; sign-in = Google or guest; tone = neon arcade energy, readable during long sessions; anti-goals = no real-money/casino-gambling cues, no fake stats, no glow on resting UI, no text under 12px. Save it at repo root as `PRODUCT.md`.

- [ ] **Step 2: Start the app for screenshots.** From `frontend/`: `VITE_SERVER_HOST=https://betrix.161.118.167.148.nip.io VITE_SERVER_PORT= npm run dev -- --port 3000` (run in background). It points at the live backend, read-only use. Check the startup log for the port it actually bound.

- [ ] **Step 3: Invoke `example-skills:webapp-testing`, then capture before screenshots** of `/`, `/login`, `/lobby` (after guest login), `/profile`, and a table (`/game/<id>` after creating one) at 375x812, 768x1024 and 1280x800. Save them as `docs/redesign/before/<page>-<width>.png`.

- [ ] **Step 4: Run `impeccable` critique and then audit on those screens.** Append its top findings (a short bulleted list) to `PRODUCT.md` under `## Baseline findings`.

- [ ] **Step 5: Stop the dev server and commit.**
```bash
git add PRODUCT.md docs/redesign/before
git commit -m "docs: product brief and pre-redesign baseline

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Google ID token verifier (backend)

**Files:**
- Modify: `backend/build.gradle` (dependencies block)
- Create: `backend/src/main/java/com/example/backend/security/GoogleIdTokenVerifier.java`
- Modify: `backend/src/main/resources/application.properties`, `backend/src/main/resources/application-local.properties`, `backend/src/test/resources/application-test.properties`
- Test: `backend/src/test/java/com/example/backend/security/GoogleIdTokenVerifierTest.java`

**Interfaces:**
- Produces: `GoogleIdTokenVerifier#verify(String idToken) -> GoogleIdTokenVerifier.GoogleIdentity` and throws `org.springframework.security.authentication.BadCredentialsException` on any invalid token. `record GoogleIdentity(String sub, String email, String name, String picture)`. Package-private `static OAuth2TokenValidator<Jwt> validator(String clientId)` and constructor `GoogleIdTokenVerifier(JwtDecoder decoder)` for tests.

- [ ] **Step 1: Add the dependency.** In `backend/build.gradle`, under `implementation 'org.springframework.boot:spring-boot-starter-security'`, add:
```groovy
	implementation 'org.springframework.security:spring-security-oauth2-jose'
```
Run `./gradlew dependencies --configuration runtimeClasspath | grep oauth2-jose` and confirm it resolves.

- [ ] **Step 2: Verify the API against the real jar** (per the saved rule: javap over documentation):
```bash
JAR=$(find ~/.gradle/caches -name 'spring-security-oauth2-jose-6*.jar' | head -1)
javap -cp "$JAR" org.springframework.security.oauth2.jwt.NimbusJwtDecoder | grep -E "withJwkSetUri|setJwtValidator|decode"
javap -cp "$JAR" org.springframework.security.oauth2.jwt.JwtClaimValidator
javap -cp "$JAR" org.springframework.security.oauth2.jwt.JwtTimestampValidator
```
Expected: `withJwkSetUri(String)`, `setJwtValidator(OAuth2TokenValidator<Jwt>)`, `JwtClaimValidator(String, Predicate<T>)`, `JwtTimestampValidator()`. If a signature differs, adapt the code below to the real one.

- [ ] **Step 3: Write the failing test.**
```java
package com.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT = "client-123.apps.googleusercontent.com";
    private final OAuth2TokenValidator<Jwt> validator = GoogleIdTokenVerifier.validator(CLIENT);

    private static Jwt token(Map<String, Object> overrides) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .audience(List.of(CLIENT))
                .subject("1234567890")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", "alice@example.com")
                .claim("email_verified", true)
                .claim("name", "Alice Doe")
                .claim("picture", "https://lh3.googleusercontent.com/a/alice");
        overrides.forEach(b::claim);
        return b.build();
    }

    @Test
    void acceptsAValidGoogleToken() {
        assertFalse(validator.validate(token(Map.of())).hasErrors());
    }

    @Test
    void acceptsTheBareIssuerVariant() {
        assertFalse(validator.validate(token(Map.of("iss", "accounts.google.com"))).hasErrors());
    }

    @Test
    void rejectsATokenForAnotherClient() {
        assertTrue(validator.validate(token(Map.of("aud", List.of("someone-else")))).hasErrors());
    }

    @Test
    void rejectsAnotherIssuer() {
        assertTrue(validator.validate(token(Map.of("iss", "https://evil.example"))).hasErrors());
    }

    @Test
    void rejectsAnUnverifiedEmail() {
        assertTrue(validator.validate(token(Map.of("email_verified", false))).hasErrors());
    }

    @Test
    void rejectsAnExpiredToken() {
        Jwt expired = token(Map.of("exp", Instant.now().minusSeconds(3600), "iat", Instant.now().minusSeconds(7200)));
        assertTrue(validator.validate(expired).hasErrors());
    }

    @Test
    void verifyMapsTheClaims() {
        var verifier = new GoogleIdTokenVerifier(t -> token(Map.of()));

        var id = verifier.verify("anything");

        assertEquals("1234567890", id.sub());
        assertEquals("alice@example.com", id.email());
        assertEquals("Alice Doe", id.name());
        assertEquals("https://lh3.googleusercontent.com/a/alice", id.picture());
    }

    @Test
    void verifyTurnsDecoderFailuresIntoBadCredentials() {
        var verifier = new GoogleIdTokenVerifier(t -> { throw new BadJwtException("bad signature"); });

        assertThrows(BadCredentialsException.class, () -> verifier.verify("forged"));
    }

    @Test
    void aBlankClientIdFailsAtStartup() {
        assertThrows(IllegalStateException.class, () -> new GoogleIdTokenVerifier(" "));
    }
}
```

- [ ] **Step 4: Run it and confirm it fails.** `./gradlew test --tests 'com.example.backend.security.GoogleIdTokenVerifierTest'`. Expected: compilation failure, `GoogleIdTokenVerifier` not found.

- [ ] **Step 5: Implement.**
```java
package com.example.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Checks a Google Sign-In ID token: signature against Google's published keys, issuer, audience
 * (our OAuth client id), expiry and a verified email. Anything else is a BadCredentialsException,
 * never a partial login.
 */
@Component
public class GoogleIdTokenVerifier {

    public record GoogleIdentity(String sub, String email, String name, String picture) {
    }

    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder decoder;

    @Autowired
    public GoogleIdTokenVerifier(@Value("${google.client-id:}") String clientId) {
        this(googleDecoder(clientId));
    }

    GoogleIdTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /** Fail at startup, not on the first sign-in, same as the JWT secret check. */
    private static JwtDecoder googleDecoder(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("google.client-id must be set (env GOOGLE_CLIENT_ID): the OAuth web client id from Google Cloud Console");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS).build();
        decoder.setJwtValidator(validator(clientId));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, iss -> iss != null && ISSUERS.contains(iss.toString())),
                new JwtClaimValidator<Object>(JwtClaimNames.AUD, aud -> aud instanceof Collection<?> c
                        ? c.contains(clientId) : clientId.equals(aud)),
                new JwtClaimValidator<Object>("email_verified", v -> Boolean.TRUE.equals(v) || "true".equals(v)));
    }

    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid Google ID token", e);
        }
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new BadCredentialsException("Google ID token has no subject");
        }
        return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
    }
}
```

- [ ] **Step 6: Add config.** In `application.properties`, append:
```properties

# Google Sign-In: the OAuth 2.0 web client id (Google Cloud Console). Required; startup fails if blank.
google.client-id=${GOOGLE_CLIENT_ID:}
# Comma-separated Google account emails that get the ADMIN role at sign-in
app.admin-emails=${APP_ADMIN_EMAILS:}
```
In `application-test.properties`, append:
```properties
google.client-id=test-client-id.apps.googleusercontent.com
app.admin-emails=admin@example.com
```
In `application-local.properties`, append:
```properties
google.client-id=${GOOGLE_CLIENT_ID:local-dev-set-GOOGLE_CLIENT_ID-to-sign-in}
```

- [ ] **Step 7: Run the test and the full suite.** `./gradlew test --tests 'com.example.backend.security.GoogleIdTokenVerifierTest'` should pass. Then `./gradlew test`: all green (`contextLoads` still boots because the test profile sets a client id).

- [ ] **Step 8: Commit.**
```bash
git add backend
git commit -m "feat(auth): verify Google ID tokens with Nimbus against Google's JWKS

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `googleLogin` mutation and user upsert (backend)

**Files:**
- Modify: `backend/src/main/java/com/example/backend/entity/User.java`
- Modify: `backend/src/main/java/com/example/backend/repository/UserRepository.java`
- Modify: `backend/src/main/java/com/example/backend/service/UserService.java`
- Modify: `backend/src/main/java/com/example/backend/resolver/MutationResolver.java`
- Modify: `backend/src/main/resources/graphql/schema.graphqls`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/com/example/backend/service/UserServiceGoogleSignInTest.java`

**Interfaces:**
- Consumes: `GoogleIdTokenVerifier#verify`, `GoogleIdentity` (Task 2).
- Produces: GraphQL `googleLogin(idToken: String!): AuthPayload!` returning `{ token, type: "Bearer" }`. `User.avatarUrl` exposed in the schema. `UserService#signInWithGoogle(GoogleIdentity) -> User`. Google users get username `google-<sub>`; their display name is `User.name`.

- [ ] **Step 1: Write the failing test.**
```java
package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.GoogleIdTokenVerifier.GoogleIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceGoogleSignInTest {

    private UserRepository repo;
    private UserService service;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new UserService(repo, "Admin@Example.com, other@example.com");
    }

    @Test
    void firstSignInCreatesAUserKeyedByGoogleSub() {
        when(repo.findByGoogleSub("sub-1")).thenReturn(Optional.empty());

        User u = service.signInWithGoogle(new GoogleIdentity("sub-1", "alice@example.com", "Alice", "https://pic/a"));

        assertEquals("sub-1", u.getGoogleSub());
        assertEquals("google-sub-1", u.getUsername());
        assertEquals("Alice", u.getName());
        assertEquals("alice@example.com", u.getEmail());
        assertEquals("https://pic/a", u.getAvatarUrl());
        assertEquals(List.of("USER"), u.getRoles());
    }

    @Test
    void laterSignInsRefreshProfileButKeepTheSameAccount() {
        User existing = new User();
        existing.setId("u1");
        existing.setGoogleSub("sub-1");
        existing.setUsername("google-sub-1");
        existing.setName("Old Name");
        existing.setHandsPlayed(12);
        when(repo.findByGoogleSub("sub-1")).thenReturn(Optional.of(existing));

        User u = service.signInWithGoogle(new GoogleIdentity("sub-1", "alice@example.com", "Alice New", "https://pic/b"));

        assertEquals("u1", u.getId());
        assertEquals("Alice New", u.getName());
        assertEquals("https://pic/b", u.getAvatarUrl());
        assertEquals(12, u.getHandsPlayed());
    }

    @Test
    void anEmailOnTheAdminListGetsTheAdminRoleCaseInsensitively() {
        when(repo.findByGoogleSub("sub-2")).thenReturn(Optional.empty());

        User u = service.signInWithGoogle(new GoogleIdentity("sub-2", "admin@example.com", "Boss", null));

        assertEquals(List.of("ADMIN"), u.getRoles());
    }

    @Test
    void aMissingNameFallsBackToAPlaceholder() {
        when(repo.findByGoogleSub("sub-3")).thenReturn(Optional.empty());

        User u = service.signInWithGoogle(new GoogleIdentity("sub-3", "x@example.com", "  ", null));

        assertEquals("Player", u.getName());
    }
}
```

- [ ] **Step 2: Run it and confirm it fails.** `./gradlew test --tests 'com.example.backend.service.UserServiceGoogleSignInTest'`. Expected: compile errors (`findByGoogleSub`, `signInWithGoogle`, `getGoogleSub`, `getAvatarUrl`, the two-arg constructor).

- [ ] **Step 3: Update `User`.** Add these fields after `email`. Leave `password` in place for now; Task 4 removes it.
```java
    @org.springframework.data.mongodb.core.index.Indexed(unique = true, sparse = true)
    private String googleSub;
    private String avatarUrl;
```
In `application.properties`, below the `spring.data.mongodb.uri` line, add:
```properties
# Creates the unique googleSub index declared on User
spring.data.mongodb.auto-index-creation=true
```

- [ ] **Step 4: Update `UserRepository`.** Add:
```java
    Optional<User> findByGoogleSub(String googleSub);
```

- [ ] **Step 5: Add `signInWithGoogle` to `UserService`.** Switch from `@RequiredArgsConstructor` to an explicit constructor. Keep `createUser` and `passwordEncoder` for now; Task 4 deletes them. Target shape:
```java
@Service
public class UserService implements UserDetailsService {

    // ...existing RESERVED, TAKEN constants stay until Task 4...
    static final String GOOGLE_PREFIX = "google-";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // removed in Task 4
    private final Set<String> adminEmails;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Value("${app.admin-emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.strip().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    UserService(UserRepository userRepository, String adminEmails) {
        this(userRepository, null, adminEmails);
    }

    /** Find the account for this Google identity, or create it; refresh profile fields and role every time. */
    public User signInWithGoogle(GoogleIdentity id) {
        User user = userRepository.findByGoogleSub(id.sub()).orElseGet(() -> {
            User u = new User();
            u.setGoogleSub(id.sub());
            u.setUsername(GOOGLE_PREFIX + id.sub());
            u.setRoles(List.of("USER"));
            return u;
        });
        user.setName(id.name() == null || id.name().isBlank() ? "Player" : id.name().strip());
        user.setEmail(id.email());
        user.setAvatarUrl(id.picture());
        boolean admin = id.email() != null && adminEmails.contains(id.email().toLowerCase(Locale.ROOT));
        user.setRoles(List.of(admin ? "ADMIN" : "USER"));
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // two first sign-ins raced; the unique googleSub index decided, use the winner's row
            return userRepository.findByGoogleSub(id.sub()).orElseThrow(() -> e);
        }
    }
    // ...loadUserByUsername and createUser unchanged...
}
```
Imports to add: `org.springframework.beans.factory.annotation.Autowired`, `org.springframework.beans.factory.annotation.Value`, `com.example.backend.security.GoogleIdTokenVerifier.GoogleIdentity`, `java.util.Arrays`, `java.util.List`, `java.util.stream.Collectors`.

- [ ] **Step 6: Run the unit test.** `./gradlew test --tests 'com.example.backend.service.UserServiceGoogleSignInTest'`. Expected: PASS.

- [ ] **Step 7: Add the mutation.** In `schema.graphqls`, inside `type Mutation`, next to `guestLogin`:
```graphql
  """Sign in with a Google ID token (from Google Identity Services). Returns JWT."""
  googleLogin(idToken: String!): AuthPayload!
```
In `type User`, after `email: String`, add `avatarUrl: String`.
In `MutationResolver`, add the field `private final GoogleIdTokenVerifier googleIdTokenVerifier;` (after `authRateLimiter`, so the Lombok constructor appends it last), and this method next to `guestLogin`:
```java
    @MutationMapping
    public Map<String, Object> googleLogin(@Argument String idToken) {
        authRateLimiter.check(ClientIp.current());
        GoogleIdTokenVerifier.GoogleIdentity identity;
        try {
            identity = googleIdTokenVerifier.verify(idToken);
        } catch (BadCredentialsException e) {
            throw GraphqlErrorException.newErrorException()
                    .message("Google sign-in failed. Try again.")
                    .errorClassification(ErrorType.UNAUTHORIZED)
                    .build();
        }
        User user = userService.signInWithGoogle(identity);
        String jwt = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return Map.of("token", jwt, "type", "Bearer");
    }
```
Add the import `com.example.backend.security.GoogleIdTokenVerifier`. In `ResolverAuthorizationTest.setUp`, append `, mock(GoogleIdTokenVerifier.class)` to the `new MutationResolver(...)` arguments, and add the import.

- [ ] **Step 8: Add an API-level test** to `AuthApiTest` (the real `NimbusJwtDecoder` rejects a malformed token while parsing, before any network call):
```java
    @Test
    void aMalformedGoogleTokenIsUnauthorized() throws Exception {
        String body = graphql("mutation { googleLogin(idToken: \"not-a-jwt\") { token } }");

        assertTrue(body.contains("\"classification\":\"UNAUTHORIZED\""), body);
        assertTrue(body.contains("Google sign-in failed"), body);
    }
```

- [ ] **Step 9: Run the full suite.** `./gradlew test`. Expected: all green.

- [ ] **Step 10: Commit.**
```bash
git add backend
git commit -m "feat(auth): googleLogin mutation, users keyed by Google sub, admin by email list

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Remove password auth (backend)

**Files:**
- Delete: `model/LoginInput.java`, `model/RegisterInput.java`, `config/DataSeeder.java`
- Delete tests: `model/RegisterInputValidationTest.java`, `service/UserServiceRegistrationTest.java`, `config/DataSeederTest.java`
- Modify: `entity/User.java`, `service/UserService.java`, `repository/UserRepository.java`, `config/SecurityConfig.java`, `resolver/MutationResolver.java`, `schema.graphqls`, `application-local.properties`, `META-INF/additional-spring-configuration-metadata.json`, `application-test.properties`, `AuthApiTest.java`, `ResolverAuthorizationTest.java`

**Interfaces:**
- Produces: the schema no longer has `login`, `register`, `LoginInput` or `RegisterInput`. `UserService(UserRepository, @Value String adminEmails)` is the only constructor.

- [ ] **Step 1: Rewrite the rate-limit test first** so it no longer uses `register`. In `AuthApiTest`, delete `invalidRegistrationIsABadRequestThatSaysWhichField` and replace `authAttemptsFromOneIpAreRateLimited` with:
```java
    @Test
    void authAttemptsFromOneIpAreRateLimited() throws Exception {
        String google = "mutation { googleLogin(idToken: \"x\") { token } }";

        graphql(google);
        graphql(google);
        String third = graphql(google);

        assertTrue(third.contains("\"classification\":\"TOO_MANY_REQUESTS\""), third);
        assertTrue(third.contains("Too many attempts"), third);
    }
```
Update the class Javadoc to: `/** Google sign-in, CORS and rate limiting through the real security chain and GraphQL endpoint. */`. `aMalformedGoogleTokenIsUnauthorized` and this test share one in-memory limiter bucket (limit 2 per minute for this class), and JUnit's method order is not fixed. Annotate the class with `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)` (import `org.springframework.test.annotation.DirtiesContext`) so every test starts with a fresh limiter.

- [ ] **Step 2: Delete the password code.**
  - Delete `LoginInput.java`, `RegisterInput.java`, `DataSeeder.java` and the three tests listed above.
  - `schema.graphqls`: remove the `login(...)` and `register(...)` fields and the `input LoginInput` / `input RegisterInput` blocks.
  - `MutationResolver`: remove `login`, `register`, the `AuthenticationManager` field, and the now-unused imports (`LoginInput`, `RegisterInput`, `Valid`, `AuthenticationManager`, `BadCredentialsException` only if unused; `googleLogin` still uses it, so keep that one).
  - `SecurityConfig`: remove the `passwordEncoder()` and `authenticationManager(...)` beans and their imports.
  - `UserService`: remove `RESERVED`, `TAKEN`, `createUser`, the `PasswordEncoder` field and the 3-arg constructor. Make `UserService(UserRepository userRepository, @Value("${app.admin-emails:}") String adminEmails)` the single public constructor (no `@Autowired` needed with one constructor).
  - `UserRepository`: remove `existsByUsernameIgnoreCase` and `existsByEmailIgnoreCase`.
  - `User`: delete the `password` field and add, because `UserDetails` requires it:
    ```java
    /** Google or guest sign-in only: there is no password. */
    @Override
    public String getPassword() {
        return null;
    }
    ```
  - `application-local.properties`: delete the three `admin.*` lines.
  - `additional-spring-configuration-metadata.json`: delete the three `admin.*` entries, keeping the JSON valid.
  - `application-test.properties`: delete `app.seed-admin=false`.
  - `ResolverAuthorizationTest`: drop the `mock(AuthenticationManager.class)` argument and its import.
- [ ] **Step 3: Search for leftovers.** `grep -rn -i "password\|LoginInput\|RegisterInput\|seed-admin\|DataSeeder\|createUser" backend/src`. Expected: only `getPassword()` in `User.java`, the `ResolverAuthorizationTest`/`GameSummaryResolverTest` `UsernamePasswordAuthenticationToken(..., "x", ...)` test credentials, and `UsernamePasswordAuthenticationToken` usages. Nothing else.

- [ ] **Step 4: Run the full suite.** `./gradlew test`. Expected: green. The count drops by the deleted tests and gains the new ones.

- [ ] **Step 5: Commit and push.**
```bash
git add -A backend
git commit -m "feat(auth)!: remove username/password login, registration and admin seeding

Google sign-in and guest are the only ways in. Existing password accounts are
intentionally dropped.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push origin redesign
```

---

### Task 5: Design tokens, fonts, primitives

**Files:**
- Modify: `frontend/src/index.css` (the `@theme` block, `@layer utilities`, keyframes)
- Modify: `frontend/index.html` (Google Fonts link)
- Modify: `frontend/src/lib/utils.js` (`formatChips`, `formatBlinds`)
- Modify: `frontend/src/components/ui/button.jsx`, `badge.jsx`, `input.jsx`, `dialog.jsx`, `sheet.jsx`, `tabs.jsx`, `slider.jsx`, `progress.jsx`, `skeleton.jsx`, `dropdown-menu.jsx`
- Test: `frontend/src/lib/__tests__/formatChips.test.js`

**Interfaces:**
- Produces Tailwind tokens used by every later task: colors `background, surface, surface-elevated, surface-overlay, border, border-strong, text, text-muted, text-dim, text-inverse, neon-cyan, neon-magenta, danger, success, warning, table-floor, table-rail, chip-1..chip-4`; fonts `font-display, font-sans, font-mono`; CSS vars `--glow-sm/md/lg`, `--glow-magenta-md`, `--ease-out-expo`; utilities `glow-cyan`, `glow-magenta`, `neon-flicker`, `tabular`. Button variants `default` (cyan filled), `outline` (cyan outline), `ghost`, `danger`, `success`, `surface`; sizes `sm, default, lg, xl, icon, icon-sm`.
- `formatChips(n)`: `null`/`undefined` → `'—'`, `950` → `'950'`, `12500` → `'12.5K'`, `2000000` → `'2M'`. `formatBlinds(10, 20)` → `'10/20'`.

- [ ] **Step 1: Invoke `ui-ux-pro-max:design-system`** with the token roles from spec §1. Check each text/background pair for WCAG AA and adjust only values that fail, keeping the roles.

- [ ] **Step 2: Write the failing test.**
```js
import { describe, expect, it } from 'vitest';
import { formatBlinds, formatChips } from '../utils';

describe('formatChips', () => {
  it('shows play-money chips without a currency sign', () => {
    expect(formatChips(950)).toBe('950');
    expect(formatChips(12500)).toBe('12.5K');
    expect(formatChips(10000)).toBe('10K');
    expect(formatChips(2000000)).toBe('2M');
    expect(formatChips(0)).toBe('0');
    expect(formatChips(null)).toBe('—');
  });

  it('formats blinds without a currency sign', () => {
    expect(formatBlinds(10, 20)).toBe('10/20');
  });
});
```
Run `npx vitest run src/lib/__tests__/formatChips.test.js`. Expected: FAIL (`$950`).

- [ ] **Step 3: Implement.**
```js
function compact(n, div, suffix) {
  return `${(n / div).toFixed(1).replace(/\.0$/, '')}${suffix}`;
}

// Play money: plain chip counts, never a currency sign.
export function formatChips(amount) {
  if (amount == null) return '—';
  if (amount >= 1_000_000) return compact(amount, 1_000_000, 'M');
  if (amount >= 1_000)     return compact(amount, 1_000, 'K');
  return amount.toLocaleString();
}

export function formatBlinds(small, big) {
  return `${small}/${big}`;
}
```
Run the test again. Expected: PASS.

- [ ] **Step 4: Replace the `@theme` block** in `index.css` (values from Step 1; defaults below come from the spec):
```css
@theme {
  --color-background: #07070f;
  --color-surface: #0e0e1a;
  --color-surface-elevated: #161628;
  --color-surface-overlay: #1f1f38;
  --color-border: rgb(255 255 255 / 0.08);
  --color-border-strong: rgb(255 255 255 / 0.14);

  --color-text: #f2f3ff;
  --color-text-muted: #a3a6c8;
  --color-text-dim: #7c80a6;
  --color-text-inverse: #07070f;

  --color-neon-cyan: #22e4ff;
  --color-neon-magenta: #ff2bd6;

  --color-danger: #ff4d6d;
  --color-success: #3dffa0;
  --color-warning: #ffb020;

  --color-table-floor: #0b0b1c;
  --color-table-rail: #22e4ff;
  --color-chip-1: #3dffa0;
  --color-chip-2: #22e4ff;
  --color-chip-3: #ff2bd6;
  --color-chip-4: #ffb020;

  --font-display: 'Space Grotesk', system-ui, sans-serif;
  --font-sans: 'Inter', system-ui, -apple-system, sans-serif;
  --font-mono: 'JetBrains Mono', ui-monospace, monospace;

  --radius-xs: 0.25rem;
  --radius-sm: 0.375rem;
  --radius: 0.5rem;
  --radius-md: 0.625rem;
  --radius-lg: 0.75rem;
  --radius-xl: 1rem;
  --radius-2xl: 1.5rem;
  --radius-full: 9999px;

  --ease-out-expo: cubic-bezier(0.16, 1, 0.3, 1);
}

:root {
  --glow-sm: 0 0 6px color-mix(in oklab, var(--color-neon-cyan) 55%, transparent);
  --glow-md: 0 0 14px color-mix(in oklab, var(--color-neon-cyan) 50%, transparent);
  --glow-lg: 0 0 28px color-mix(in oklab, var(--color-neon-cyan) 45%, transparent);
  --glow-magenta-md: 0 0 16px color-mix(in oklab, var(--color-neon-magenta) 55%, transparent);
}
```
In `@layer utilities`, delete `felt-surface`, `seat-pulse`, `winner-glow` and any `chip-shine`, and add:
```css
  .glow-cyan    { box-shadow: var(--glow-md); }
  .glow-magenta { box-shadow: var(--glow-magenta-md); }
  .tabular      { font-variant-numeric: tabular-nums; }
  .neon-flicker { animation: neonFlicker 6s linear infinite; }
```
Add the keyframes, and remove any keyframes nothing uses any more (`seatPulse`, `winnerGlow`):
```css
@keyframes neonFlicker {
  0%, 92%, 100% { opacity: 1; }
  93% { opacity: 0.55; }
  94% { opacity: 1; }
  96% { opacity: 0.7; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```
Add a global focus style:
```css
:focus-visible {
  outline: 2px solid var(--color-neon-cyan);
  outline-offset: 2px;
  box-shadow: var(--glow-sm);
}
```

- [ ] **Step 5: Fonts.** In `frontend/index.html`, replace the Google Fonts `<link href=...>` with:
```html
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet" />
```
Remove the `.font-serif` utility from `index.css`, then `grep -rn "font-serif" src` and change each hit to `font-display`.

- [ ] **Step 6: Restyle `components/ui/*`.**
  - `button.jsx` variants:
    - `default`: `bg-neon-cyan text-text-inverse hover:shadow-[var(--glow-md)]`
    - `outline`: `border border-neon-cyan text-neon-cyan bg-transparent hover:bg-neon-cyan/10`
    - `ghost`: `text-text-muted hover:text-text hover:bg-surface-elevated`
    - `danger`: `bg-danger/15 text-danger border border-danger/40`
    - `success`: `bg-success/15 text-success border border-success/40`
    - `surface`: `bg-surface-elevated text-text border border-border-strong`
  - Sizes: `sm` h-9, `default` h-11, `lg` h-12, `xl` h-14, `icon` size-11, `icon-sm` size-9 (all at least 36px; the 44px targets come from `default` and up).
  - `input`: `bg-surface border-border-strong focus-visible:border-neon-cyan`.
  - `badge`: keep the variants `active`/`waiting`/`completed`; map them to cyan/warning/text-dim tints (`bg-<color>/15 text-<color>`).
  - `dialog`/`sheet`/`dropdown-menu`: `bg-surface-elevated border-border-strong`, with an overlay of `bg-black/70 backdrop-blur-sm`.
  - `tabs`: the active trigger gets `text-neon-cyan` plus a 2px cyan bottom border.
  - `slider`: cyan range, a thumb with `var(--glow-sm)`.
  - `skeleton`: `bg-surface-elevated animate-pulse`.

  Remove every hardcoded hex and every `text-[8-11px]` from these files.

- [ ] **Step 7: Check for leftovers.** Run `grep -rnE "#[0-9a-fA-F]{3,8}\b|text-\[(8|9|10|11)px\]|\bgold\b|felt|rim-" src --include=*.jsx`. The remaining hits belong to page and poker components that later tasks rewrite; list them in the commit body so Tasks 7-14 clear them.

- [ ] **Step 8: Run the checks.** Run `npx vitest run` and `npm run build`. Both must pass.

- [ ] **Step 9: Commit.**
```bash
git add frontend
git commit -m "feat(ui): neon design tokens, fonts, restyled primitives; chips without currency

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Frontend auth: Google + guest Home, session expiry

**Files:**
- Modify: `frontend/src/context/AuthContext.jsx`
- Modify: `frontend/src/hooks/useAuth.js`
- Modify: `frontend/src/graphql/mutations.js`, `frontend/src/graphql/queries.js`
- Create: `frontend/src/components/auth/GoogleSignInButton.jsx`
- Rewrite: `frontend/src/pages/Home.jsx`
- Delete: `frontend/src/pages/Login.jsx`, `frontend/src/pages/Register.jsx`, `frontend/src/pages/__tests__/Register.test.jsx`
- Modify: `frontend/src/App.jsx`, `frontend/src/components/routing/ProtectedRoute.jsx`, `frontend/src/components/routing/__tests__/ProtectedRoute.test.jsx`
- Modify: `.github/workflows/deploy-frontend-firebase.yml` (build env)
- Test: `frontend/src/pages/__tests__/Home.test.jsx`, `frontend/src/context/__tests__/AuthContext.test.jsx`

**Interfaces:**
- Consumes: the backend `googleLogin(idToken)` mutation (Task 3) and `User.avatarUrl`.
- Produces: `useAuth()` returning `{ guestLogin(): Promise<{token}>, googleLogin(idToken): Promise<{token}> }`. `AuthContext` value `{ isLoggedIn, user, login(token), logout(), refreshUserData() }` (same keys as today). `<GoogleSignInButton onCredential={(idToken) => void} onError={(msg) => void} />`. Logged-out visitors to protected routes are redirected to `/` (Home), not `/login`.

- [ ] **Step 1: Write the failing tests.** `pages/__tests__/Home.test.jsx`:
```jsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import Home from '../Home';

const guestLogin = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ guestLogin, googleLogin: vi.fn() }),
}));
vi.mock('@/components/auth/GoogleSignInButton', () => ({
  default: () => <button type="button">Continue with Google</button>,
}));

function renderHome(ctx = {}) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ isLoggedIn: false, login: vi.fn(), ...ctx }}>
        <Home />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('Home', () => {
  it('offers exactly Google sign-in and guest play', () => {
    renderHome();
    expect(screen.getByRole('button', { name: /continue with google/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /play as guest/i })).toBeInTheDocument();
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
  });

  it('shows an error instead of crashing when guest login fails', async () => {
    guestLogin.mockRejectedValueOnce(new Error('Guest login failed.'));
    const user = userEvent.setup();
    renderHome();

    await user.click(screen.getByRole('button', { name: /play as guest/i }));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Guest login failed.'));
  });

  it('hands a successful guest token to AuthContext', async () => {
    guestLogin.mockResolvedValueOnce({ token: 'jwt-1' });
    const login = vi.fn();
    const user = userEvent.setup();
    renderHome({ login });

    await user.click(screen.getByRole('button', { name: /play as guest/i }));

    await waitFor(() => expect(login).toHaveBeenCalledWith('jwt-1'));
  });
});
```
`context/__tests__/AuthContext.test.jsx`:
```jsx
import { render, screen, waitFor } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing/react';
import { useContext } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AuthProvider, { AuthContext } from '../AuthContext';
import { GET_ME } from '@/graphql/queries';

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

function Probe() {
  const { isLoggedIn, user } = useContext(AuthContext);
  return <div>{isLoggedIn ? `in:${user?.name ?? '-'}` : 'out'}</div>;
}

const meMock = (me) => ({ request: { query: GET_ME }, result: { data: { me } } });

describe('AuthProvider', () => {
  beforeEach(() => localStorage.clear());

  it('loads the user for a stored token', async () => {
    localStorage.setItem('token', 't');
    const me = { __typename: 'User', id: 'u1', name: 'Alice', username: 'google-1', email: null, avatarUrl: null,
      roles: ['USER'], handsPlayed: 0, handsWon: 0, winRate: 0, netProfit: 0 };
    render(<MockedProvider mocks={[meMock(me)]}><AuthProvider><Probe /></AuthProvider></MockedProvider>);

    await waitFor(() => expect(screen.getByText('in:Alice')).toBeInTheDocument());
  });

  it('logs out when the stored token no longer resolves to a user', async () => {
    localStorage.setItem('token', 'expired');
    render(<MockedProvider mocks={[meMock(null)]}><AuthProvider><Probe /></AuthProvider></MockedProvider>);

    await waitFor(() => expect(screen.getByText('out')).toBeInTheDocument());
    expect(localStorage.getItem('token')).toBeNull();
  });
});
```
Before relying on it, check the `MockedProvider` import path: `ls node_modules/@apollo/client/testing/react`. If that path doesn't exist in 4.1.6, use the path the package exports (`grep -n MockedProvider node_modules/@apollo/client/testing/**/*.d.ts`).

Run `npx vitest run src/pages/__tests__/Home.test.jsx src/context/__tests__/AuthContext.test.jsx`. Expected: FAIL.

- [ ] **Step 2: GraphQL documents.** In `mutations.js`, delete `LOGIN` and `REGISTER` and add:
```js
export const GOOGLE_LOGIN = gql`
  mutation GoogleLogin($idToken: String!) {
    googleLogin(idToken: $idToken) {
      token
      type
    }
  }
`;
```
In `queries.js` `GET_ME`, add `avatarUrl` after `email`.

- [ ] **Step 3: `useAuth.js`.**
```js
import { useMutation } from '@apollo/client/react';
import { GOOGLE_LOGIN, GUEST_LOGIN } from '../graphql/mutations';

/**
 * Thin wrappers around the sign-in mutations. Storing the token and loading the user is
 * AuthContext's job; callers hand the returned token to AuthContext's login().
 */
export function useAuth() {
  const [guestLoginMutation] = useMutation(GUEST_LOGIN);
  const [googleLoginMutation] = useMutation(GOOGLE_LOGIN);

  const guestLogin = async () => (await guestLoginMutation()).data.guestLogin;
  const googleLogin = async (idToken) =>
    (await googleLoginMutation({ variables: { idToken } })).data.googleLogin;

  return { guestLogin, googleLogin };
}
```

- [ ] **Step 4: `AuthContext.jsx`.** Apollo 4 ignores `onCompleted`/`onError`, so the query result was never applied. Replace the `useQuery` block and the mount `useEffect` with:
```jsx
  const hasToken = !!localStorage.getItem('token');
  const { data: meData, error: meError, refetch: refetchMe } = useQuery(GET_ME, { skip: !hasToken });

  useEffect(() => {
    if (!hasToken || (!meData && !meError)) return;
    if (meData?.me) {
      setUser(meData.me);
      localStorage.setItem('user', JSON.stringify(meData.me));
      return;
    }
    // A token the server no longer accepts (expired, signed with a rotated secret, deleted account)
    // resolves `me` to null. Drop it instead of leaving the app half signed-in.
    logout();
    toast.error('Session expired. Sign in again.', { id: 'session-expired' });
  }, [meData, meError]); // eslint-disable-line react-hooks/exhaustive-deps
```
Add `import { toast } from 'sonner';`. Keep `login`, `logout` and `refreshUserData` as they are. `logout` is declared below this effect; function declarations via `const` are fine because the effect runs after render.

- [ ] **Step 5: `GoogleSignInButton.jsx`.**
```jsx
import React, { useEffect, useRef } from 'react';

const GSI_SRC = 'https://accounts.google.com/gsi/client';
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

function loadGsi() {
  if (window.google?.accounts?.id) return Promise.resolve();
  const existing = document.querySelector(`script[src="${GSI_SRC}"]`);
  return new Promise((resolve, reject) => {
    const s = existing || Object.assign(document.createElement('script'), { src: GSI_SRC, async: true, defer: true });
    s.addEventListener('load', resolve, { once: true });
    s.addEventListener('error', () => reject(new Error('Could not load Google sign-in.')), { once: true });
    if (!existing) document.head.appendChild(s);
  });
}

/** Google's own rendered button (brand rules), dark variant. Calls onCredential with the ID token. */
export default function GoogleSignInButton({ onCredential, onError }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!CLIENT_ID) { onError?.('Google sign-in is not configured.'); return; }
    let cancelled = false;
    loadGsi().then(() => {
      if (cancelled || !ref.current) return;
      window.google.accounts.id.initialize({
        client_id: CLIENT_ID,
        callback: ({ credential }) => onCredential(credential),
      });
      window.google.accounts.id.renderButton(ref.current, {
        theme: 'filled_black', size: 'large', shape: 'pill', text: 'continue_with', width: 320,
      });
    }).catch((e) => onError?.(e.message));
    return () => { cancelled = true; };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return <div ref={ref} className="flex justify-center min-h-11" />;
}
```

- [ ] **Step 6: `Home.jsx` (sign-in screen).** Structure:
  - A full-height centered column (`min-h-dvh flex flex-col items-center justify-center gap-10 px-4`).
  - Wordmark `BETRIX` in `font-display text-5xl font-bold text-neon-cyan neon-flicker`, with `text-shadow: var(--glow-md)` applied via inline style.
  - One line of `text-text-muted`: "Play-money Texas Hold'em with friends and bots."
  - A `w-full max-w-xs flex flex-col gap-3` containing `<GoogleSignInButton onCredential={handleGoogle} onError={setError} />`, an "or" divider, and `<Button variant="outline" size="lg" onClick={handleGuest}>Play as guest</Button>` (show `Loader2` while busy).
  - `{error && <p role="alert" className="text-danger text-sm text-center">{error}</p>}`.
  - A `useEffect` that redirects to `/lobby` when `isLoggedIn` (keep the existing one).

  Handlers:
```jsx
  const { guestLogin, googleLogin } = useAuth();
  const { isLoggedIn, login } = useContext(AuthContext);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const signIn = async (getToken) => {
    setError(null); setBusy(true);
    try {
      const { token } = await getToken();
      await login(token);
      navigate(location.state?.from || '/lobby', { replace: true });
    } catch (e) {
      setError(e.message || 'Sign-in failed. Try again.');
    } finally {
      setBusy(false);
    }
  };
  const handleGuest = () => signIn(guestLogin);
  const handleGoogle = (idToken) => signIn(() => googleLogin(idToken));
```
Delete `FEATURES`, `TablePreview` and every marketing section. The page has no Navbar (Task 7 hides the shell on `/`).

- [ ] **Step 7: Routes.**
  - `App.jsx`: remove the `Login`/`Register` imports and the `/login` and `/register` routes.
  - `ProtectedRoute.jsx`: both components redirect logged-out visitors with `<Navigate to="/" state={{ from: location.pathname }} replace />` instead of `/login`.
  - `ProtectedRoute.test.jsx`: change the expectations from 'login page' to 'home page' for logged-out cases, and drop the `/login` route from `renderAt`.
  - Delete `Login.jsx`, `Register.jsx`, `Register.test.jsx`.
  - Run `grep -rn "/login\|/register" src`: expected no hits except Navbar, which Task 7 rewrites.

- [ ] **Step 8: CI build env.** In `.github/workflows/deploy-frontend-firebase.yml`, add to the build step's `env:`:
```yaml
          VITE_GOOGLE_CLIENT_ID: ${{ vars.GOOGLE_CLIENT_ID }}
```
(A repository *variable*, not a secret: the client ID is public. Kush sets it in GitHub → Settings → Variables before merge.)

- [ ] **Step 9: Run the checks.** Run `npx vitest run` and `npm run build`. Both must be green.

- [ ] **Step 10: Commit.**
```bash
git add -A frontend .github
git commit -m "feat(auth): Google + guest sign-in home, drop login/register pages, expire stale sessions

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: App shell (top bar + bottom tab bar)

**Files:**
- Create: `frontend/src/components/layout/TopBar.jsx`, `frontend/src/components/layout/BottomTabBar.jsx`
- Delete: `frontend/src/components/layout/Navbar.jsx`
- Modify: `frontend/src/App.jsx`, `frontend/src/components/layout/PageWrapper.jsx`
- Test: `frontend/src/components/layout/__tests__/BottomTabBar.test.jsx`

**Interfaces:**
- Produces: `<TopBar />` and `<BottomTabBar />` read `AuthContext` and render nothing on `/` or `/game/*`. The pages' `PageWrapper` pads the bottom by `pb-20 lg:pb-0` so the tab bar never covers content.

- [ ] **Step 1: Write the failing test.**
```jsx
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import BottomTabBar from '../BottomTabBar';

const renderAt = (path, user) => render(
  <MemoryRouter initialEntries={[path]}>
    <AuthContext.Provider value={{ isLoggedIn: !!user, user }}>
      <BottomTabBar />
    </AuthContext.Provider>
  </MemoryRouter>
);

describe('BottomTabBar', () => {
  it('shows Lobby and Profile, and Admin only for admins', () => {
    renderAt('/lobby', { roles: ['USER'] });
    expect(screen.getByRole('link', { name: /lobby/i })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: /profile/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /admin/i })).not.toBeInTheDocument();
  });

  it('adds Admin for admins', () => {
    renderAt('/lobby', { roles: ['ADMIN'] });
    expect(screen.getByRole('link', { name: /admin/i })).toBeInTheDocument();
  });

  it('is hidden on the sign-in screen and at the table', () => {
    const { container } = renderAt('/game/g1', { roles: ['USER'] });
    expect(container).toBeEmptyDOMElement();
  });
});
```
Run it. Expected: FAIL (module missing).

- [ ] **Step 2: Implement `BottomTabBar.jsx`.**
```jsx
import React, { useContext } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { LayoutGrid, User, Shield } from 'lucide-react';
import { AuthContext } from '@/context/AuthContext';
import { cn } from '@/lib/utils';

export const isChromeless = (path) => path === '/' || path.startsWith('/game/');
export const isAdminUser = (user) => user?.roles?.some((r) => r === 'ADMIN' || r === 'ROLE_ADMIN');

export default function BottomTabBar() {
  const { isLoggedIn, user } = useContext(AuthContext);
  const { pathname } = useLocation();
  if (!isLoggedIn || isChromeless(pathname)) return null;

  const tabs = [
    { to: '/lobby', label: 'Lobby', Icon: LayoutGrid },
    { to: '/profile', label: 'Profile', Icon: User },
    ...(isAdminUser(user) ? [{ to: '/admin', label: 'Admin', Icon: Shield }] : []),
  ];

  return (
    <nav aria-label="Main" className="lg:hidden fixed bottom-0 inset-x-0 z-40 border-t border-border bg-surface/90 backdrop-blur pb-[env(safe-area-inset-bottom)]">
      <ul className="flex">
        {tabs.map(({ to, label, Icon }) => (
          <li key={to} className="flex-1">
            <NavLink to={to} className={({ isActive }) => cn(
              'flex flex-col items-center justify-center gap-1 h-16 text-xs',
              isActive ? 'text-neon-cyan' : 'text-text-muted hover:text-text')}>
              <Icon size={22} aria-hidden="true" />
              {label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
```
`NavLink` sets `aria-current="page"` on the active link automatically.

- [ ] **Step 3: Implement `TopBar.jsx`.** It returns null under the same conditions as the tab bar. Layout: `sticky top-0 z-40 h-14 flex items-center justify-between px-4 border-b border-border bg-surface/90 backdrop-blur`.
  - Left: the `BETRIX` wordmark (`font-display font-bold text-neon-cyan`) linking to `/lobby`.
  - Middle, `hidden lg:flex`: the same links as the tab bar, reusing `isAdminUser`.
  - Right: an avatar dropdown (existing `ui/dropdown-menu` + `ui/avatar`) that uses `user.avatarUrl` when present and falls back to `getPlayerInitials(user.name)`. Items: Profile, Admin (admins only), Sign out (`logout()` then `navigate('/')`).

  No chip count: chips are per table.

- [ ] **Step 4: Wire it up.** In `App.jsx`, replace `<Navbar />` with `<TopBar />`, render `<BottomTabBar />` after `<Routes>`, and move the `Toaster` to `position="top-center"`. Delete `Navbar.jsx`. In `PageWrapper.jsx`, add `pb-20 lg:pb-0` to its root class.

- [ ] **Step 5: Run the checks.** Run `npx vitest run` and `npm run build`. Both must be green.

- [ ] **Step 6: Commit.**
```bash
git add -A frontend
git commit -m "feat(ui): mobile tab bar and slim top bar replace the navbar

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Lobby, Profile, Admin, NotFound

**Files:**
- Modify: `frontend/src/components/lobby/GameCard.jsx` (becomes a row), `frontend/src/components/lobby/__tests__/GameCard.test.jsx`
- Modify: `frontend/src/pages/GameLobby.jsx`, `frontend/src/components/lobby/CreateGameModal.jsx`, `frontend/src/components/lobby/EmptyLobbyState.jsx`
- Modify: `frontend/src/pages/Profile.jsx`, `frontend/src/pages/__tests__/Profile.test.jsx`
- Modify: `frontend/src/pages/AdminPanel.jsx`, `frontend/src/pages/NotFound.jsx`

**Interfaces:**
- Consumes: `GET_GAMES` fields (`id, status, playerCount, maxPlayers, smallBlindAmount, bigBlindAmount, isYourGame`); `formatBlinds`; `STATUS_LABELS` and `STATUS_BADGE_VARIANT` from `lib/utils.js`.
- Produces: `GameCard` keeps its props (`game`, `onJoin`) so `GameLobby` and its test keep working.

- [ ] **Step 1: Extend the GameCard test first.** Keep the existing cases, then add:
```jsx
  it('renders one seat dot per seat, filled for taken seats', () => {
    render(<MemoryRouter><GameCard game={baseGame} onJoin={() => {}} /></MemoryRouter>);
    const dots = screen.getAllByTestId('seat-dot');
    expect(dots).toHaveLength(6);
    expect(dots.filter((d) => d.dataset.filled === 'true')).toHaveLength(2);
  });

  it('shows blinds without a currency sign', () => {
    render(<MemoryRouter><GameCard game={baseGame} onJoin={() => {}} /></MemoryRouter>);
    expect(screen.getByText('10/20')).toBeInTheDocument();
    expect(screen.queryByText(/\$/)).not.toBeInTheDocument();
  });
```
Match the wrapper and imports the existing test file already uses. Run it. Expected: the new cases FAIL.

- [ ] **Step 2: GameCard as a row.** Build it as a `<li>`-friendly `div` styled `flex items-center gap-3 p-3 rounded-xl bg-surface border border-border`, plus `border-neon-cyan/40` when `isYourGame`. Contents:
  - Left block: table label `Table #{id.slice(-4)}` (`font-display`) and a line with blinds in `font-mono tabular` + the status `Badge`.
  - Middle: `maxPlayers` dots, each a `<span data-testid="seat-dot" data-filled={i < playerCount}>` of size 8px. Filled dots are `bg-neon-cyan`, empty ones `bg-border-strong`. Add `aria-label={`${playerCount} of ${maxPlayers} seats taken`}` on the container.
  - Right: the action button, keeping today's logic: `Rejoin` (default variant) when `isYourGame`; `Full` (disabled surface) when full; otherwise `Join` (outline).

  Remove all `gold`/hex/micro-size classes.

- [ ] **Step 3: GameLobby.** Render a `ul` of rows with `grid gap-2 md:grid-cols-2`, sorting `isYourGame` first:
```js
const sorted = [...games].sort((a, b) => Number(b.isYourGame) - Number(a.isYourGame));
```
  - The page title is `Lobby`, in `font-display text-2xl`.
  - Loading shows 4 skeleton rows of the same height (`h-16 rounded-xl`).
  - Add a sticky create button: `fixed right-4 bottom-20 lg:bottom-6 z-30` (`Button` size `lg`, with a `Plus` icon, label "Create table", `glow-cyan`). It opens `CreateGameModal`.
  - `CreateGameModal` renders inside the existing `BottomSheet` on mobile and keeps `Dialog` at `md:` and up. Use `useMediaQuery('(min-width: 768px)')` from `hooks/useMediaQuery.js`.
  - `EmptyLobbyState`: "No tables yet", with the Create button.

- [ ] **Step 4: Profile.**
  - Header: `Avatar` (uses `avatarUrl`), the name in `font-display text-2xl`, and a "Guest" chip when `roles` includes `GUEST`.
  - Stat tiles in `grid grid-cols-2 gap-3`: Hands played, Hands won, Win rate (`%`), Net result (`formatChips`, `text-success`/`text-danger` by sign). Numbers use `font-mono tabular text-2xl`.
  - Guests see a card: "Sign in with Google to keep your stats", with a `Button` that calls `logout()` and navigates to `/`.
  - Keep the skeleton-when-`user`-is-null path.
  - Test update: `Profile.test.jsx` currently expects `screen.getByText('alice')` (the username). Change the fixture to `username: 'google-1'` and assert the username is **not** shown: `expect(screen.queryByText('google-1')).not.toBeInTheDocument()`.

- [ ] **Step 5: AdminPanel.** Reuse the lobby row layout: import `GameCard`'s row styles, or render the same markup with a `Delete` danger button in place of Join. Functional styling only; remove hex and micro sizes.

- [ ] **Step 6: NotFound.** Centered column: two face-down `PokerCard`s tilted ±8°, "This hand folded" in `font-display text-2xl`, and a "Back to lobby" button.

- [ ] **Step 7: Run the checks.** Run `npx vitest run` and `npm run build`. Both must be green. Then `grep -rnE "#[0-9a-fA-F]{3,8}\b|text-\[(8|9|10|11)px\]|\bgold\b" src/pages/GameLobby.jsx src/pages/Profile.jsx src/pages/AdminPanel.jsx src/pages/NotFound.jsx src/components/lobby`: expected no hits.

- [ ] **Step 8: Commit.**
```bash
git add -A frontend
git commit -m "feat(ui): lobby rows with seat dots, stat-tile profile, admin and 404 restyle

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Connection status

**Files:**
- Modify: `frontend/src/api/apolloClient.js`
- Create: `frontend/src/hooks/useConnectionStatus.js`
- Create: `frontend/src/components/poker/ConnectionPill.jsx`
- Test: `frontend/src/hooks/__tests__/useConnectionStatus.test.jsx`

**Interfaces:**
- Produces: `export const wsClient` from `apolloClient.js`. `connectionReducer(state, event) -> 'online'|'reconnecting'|'offline'` (exported for tests). `useConnectionStatus(client = wsClient) -> 'online'|'reconnecting'|'offline'`. `<ConnectionPill status={...} />`, which renders null when `online`.

- [ ] **Step 1: Write the failing test.**
```jsx
import { act, render, renderHook, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { connectionReducer, useConnectionStatus } from '../useConnectionStatus';
import ConnectionPill from '@/components/poker/ConnectionPill';

function fakeClient() {
  const listeners = {};
  return {
    on: (event, cb) => { (listeners[event] ||= []).push(cb); return () => {}; },
    emit: (event) => (listeners[event] || []).forEach((cb) => cb()),
  };
}

describe('connectionReducer', () => {
  it('maps socket events to a status', () => {
    expect(connectionReducer('online', 'connecting')).toBe('reconnecting');
    expect(connectionReducer('reconnecting', 'connected')).toBe('online');
    expect(connectionReducer('online', 'closed')).toBe('reconnecting');
    expect(connectionReducer('reconnecting', 'browser-offline')).toBe('offline');
    expect(connectionReducer('offline', 'browser-online')).toBe('reconnecting');
    expect(connectionReducer('offline', 'closed')).toBe('offline');
  });
});

describe('useConnectionStatus', () => {
  it('follows the client events', () => {
    const client = fakeClient();
    const { result } = renderHook(() => useConnectionStatus(client));
    expect(result.current).toBe('online');
    act(() => client.emit('closed'));
    expect(result.current).toBe('reconnecting');
    act(() => client.emit('connected'));
    expect(result.current).toBe('online');
  });
});

describe('ConnectionPill', () => {
  it('is hidden when online and announces trouble otherwise', () => {
    const { container, rerender } = render(<ConnectionPill status="online" />);
    expect(container).toBeEmptyDOMElement();
    rerender(<ConnectionPill status="offline" />);
    expect(screen.getByRole('status')).toHaveTextContent(/offline/i);
  });
});
```
Run it. Expected: FAIL.

- [ ] **Step 2: Implement.** In `apolloClient.js`, pull the `createClient(...)` result into `export const wsClient = createClient({...});` and pass it to `new GraphQLWsLink(wsClient)`.

`hooks/useConnectionStatus.js`:
```js
import { useEffect, useReducer } from 'react';
import { wsClient } from '@/api/apolloClient';

// The browser being offline wins over socket events until it comes back.
export function connectionReducer(state, event) {
  switch (event) {
    case 'browser-offline': return 'offline';
    case 'browser-online':  return 'reconnecting';
    case 'connected':       return state === 'offline' ? state : 'online';
    case 'connecting':
    case 'closed':          return state === 'offline' ? state : 'reconnecting';
    default:                return state;
  }
}

export function useConnectionStatus(client = wsClient) {
  const [status, dispatch] = useReducer(connectionReducer, 'online');
  useEffect(() => {
    const offs = ['connecting', 'connected', 'closed'].map((e) => client.on(e, () => dispatch(e)));
    const goOffline = () => dispatch('browser-offline');
    const goOnline = () => dispatch('browser-online');
    window.addEventListener('offline', goOffline);
    window.addEventListener('online', goOnline);
    return () => {
      offs.forEach((off) => off());
      window.removeEventListener('offline', goOffline);
      window.removeEventListener('online', goOnline);
    };
  }, [client]);
  return status;
}
```
Note: `connected` after `browser-offline` cannot really happen, but the reducer keeps `offline` until the browser says it is back, so the pill never flickers green while the network is down.

`components/poker/ConnectionPill.jsx`:
```jsx
import React from 'react';
import { Loader2, WifiOff } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function ConnectionPill({ status }) {
  if (status === 'online') return null;
  const offline = status === 'offline';
  return (
    <div role="status" aria-live="polite" className={cn(
      'flex items-center gap-1.5 rounded-full px-3 h-8 text-xs font-medium border',
      offline ? 'bg-danger/15 text-danger border-danger/40' : 'bg-warning/15 text-warning border-warning/40')}>
      {offline ? <WifiOff size={14} aria-hidden="true" /> : <Loader2 size={14} className="animate-spin" aria-hidden="true" />}
      {offline ? 'Offline, actions paused' : 'Reconnecting…'}
    </div>
  );
}
```

- [ ] **Step 3: Run the test.** Run `npx vitest run src/hooks/__tests__/useConnectionStatus.test.jsx`: PASS. Then run `npm run build`.

- [ ] **Step 4: Commit.**
```bash
git add frontend
git commit -m "feat(table): WebSocket connection status hook and pill

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Game state reducer + `useGame` owns table logic

**Files:**
- Create: `frontend/src/lib/gameReducer.js`
- Rewrite: `frontend/src/hooks/useGame.js`
- Test: `frontend/src/lib/__tests__/gameReducer.test.js`

**Interfaces:**
- Consumes: `JOIN_GAME`, `PLAYER_ACTION`, `LEAVE_GAME`, `SIT_OUT`, `SIT_IN`, `START_HAND`, `SEND_CHAT`, `GAME_UPDATED`, `PLAYER_UPDATED` (existing GraphQL documents).
- Produces:
  - `initialTableState = { game: null, hand: [], chat: [], showdown: null }`
  - `tableReducer(state, action)` with actions `{ type: 'joined', game }` and `{ type: 'update', update: { type, payload } }`, where `update.type` is one of the backend `GameUpdateType` names.
  - `showdown` shape: `{ winners: Player[], bestHand: { rank, highCards: Card[] } | null, game }`.
  - `heroIndex(game, username) -> number` (-1 if not seated).
  - `useGame(gameId, username)` returns `{ status: 'joining'|'ready'|'error', error, game, hand, chat, showdown, clearShowdown, heroIndex, hero, isMyTurn, actions: { bet(amount), check(), fold(), leave(), sitOut(), sitIn(), startHand(), sendChat(text) } }`.

- [ ] **Step 1: Write the failing test.**
```js
import { describe, expect, it } from 'vitest';
import { heroIndex, initialTableState, tableReducer } from '../gameReducer';

const game = { id: 'g1', players: [{ id: 'p1', username: 'alice' }, { id: 'p2', username: 'bot-1' }], communityCards: [] };
const update = (type, payload) => ({ type: 'update', update: { type, payload } });
const joined = tableReducer(initialTableState, { type: 'joined', game });

describe('tableReducer', () => {
  it('stores the joined game', () => {
    expect(joined.game).toBe(game);
  });

  it('keeps the private hand from CARDS_DEALT without touching the game', () => {
    const cards = [{ suit: 'HEARTS', rank: 'ACE' }, { suit: 'SPADES', rank: 'KING' }];
    const s = tableReducer(joined, update('CARDS_DEALT', cards));
    expect(s.hand).toEqual(cards);
    expect(s.game).toBe(game);
  });

  it('appends chat from CHAT_MESSAGE (the type the backend actually sends)', () => {
    const msg = { senderId: 'p2', senderName: 'Bot', message: 'gl', timestamp: '2026-09-25T10:00:00Z' };
    expect(tableReducer(joined, update('CHAT_MESSAGE', msg)).chat).toEqual([msg]);
  });

  it('replaces community cards', () => {
    const cc = [{ suit: 'CLUBS', rank: 'TWO' }];
    expect(tableReducer(joined, update('COMMUNITY_CARDS', cc)).game.communityCards).toEqual(cc);
  });

  it('adds a joining player once, even if the event repeats', () => {
    const p3 = { id: 'p3', username: 'carol' };
    const s = tableReducer(tableReducer(joined, update('PLAYER_JOINED', p3)), update('PLAYER_JOINED', p3));
    expect(s.game.players.map((p) => p.id)).toEqual(['p1', 'p2', 'p3']);
  });

  it('takes the full game from PLAYER_ACTION, GAME_STARTED and ROUND_STARTED', () => {
    const next = { ...game, pot: 40 };
    for (const t of ['PLAYER_ACTION', 'GAME_STARTED', 'ROUND_STARTED']) {
      expect(tableReducer(joined, update(t, next)).game.pot).toBe(40);
    }
  });

  it('records a showdown from GAME_ENDED and clears the old hand', () => {
    const ended = { ...game, pot: 0 };
    const bestHand = { rank: 'FLUSH', highCards: [{ suit: 'HEARTS', rank: 'ACE' }] };
    const withHand = tableReducer(joined, update('CARDS_DEALT', [{ suit: 'HEARTS', rank: 'ACE' }]));
    const s = tableReducer(withHand, update('GAME_ENDED', { game: ended, winners: [game.players[0]], bestHand }));
    expect(s.showdown).toEqual({ game: ended, winners: [game.players[0]], bestHand });
    expect(s.game.status).toBe('ENDED');
    expect(s.hand).toEqual([]);
  });

  it('a new hand starting clears the previous showdown', () => {
    const s = tableReducer(
      tableReducer(joined, update('GAME_ENDED', { game, winners: [], bestHand: null })),
      update('GAME_STARTED', game));
    expect(s.showdown).toBeNull();
  });

  it('ignores unknown update types', () => {
    expect(tableReducer(joined, update('SOMETHING_NEW', {}))).toBe(joined);
  });
});

describe('heroIndex', () => {
  it('finds the seat by username', () => {
    expect(heroIndex(game, 'bot-1')).toBe(1);
    expect(heroIndex(game, 'nobody')).toBe(-1);
    expect(heroIndex(null, 'alice')).toBe(-1);
  });
});
```
Run it. Expected: FAIL (module missing).

- [ ] **Step 2: Implement `lib/gameReducer.js`.**
```js
export const initialTableState = { game: null, hand: [], chat: [], showdown: null };

const withGame = (state, g) => ({
  ...state,
  game: { ...g, communityCards: g?.communityCards || [], players: g?.players || [] },
});

/** Pure: every subscription event goes through here, so React StrictMode double-invocation is harmless. */
export function tableReducer(state, action) {
  if (action.type === 'joined') return withGame(state, action.game);
  if (action.type !== 'update') return state;

  const { type, payload } = action.update;
  switch (type) {
    case 'CARDS_DEALT':
      return Array.isArray(payload) ? { ...state, hand: payload } : state;
    case 'CHAT_MESSAGE':
      return payload ? { ...state, chat: [...state.chat, payload] } : state;
    case 'COMMUNITY_CARDS':
      return Array.isArray(payload) ? { ...state, game: { ...state.game, communityCards: payload } } : state;
    case 'PLAYER_JOINED': {
      const players = state.game?.players || [];
      if (!payload || players.some((p) => p.id === payload.id)) return state;
      return { ...state, game: { ...state.game, players: [...players, payload] } };
    }
    case 'PLAYER_ACTION':
      return withGame(state, payload?.game || payload);
    case 'GAME_STARTED':
    case 'ROUND_STARTED':
      return { ...withGame(state, payload), showdown: null };
    case 'GAME_ENDED': {
      const g = payload?.game || payload;
      const next = withGame(state, { ...g, status: 'ENDED' });
      return {
        ...next,
        hand: [],
        showdown: { game: g, winners: payload?.winners || [], bestHand: payload?.bestHand || null },
      };
    }
    default:
      return state;
  }
}

export function heroIndex(game, username) {
  return game?.players?.findIndex((p) => p.username === username) ?? -1;
}
```

- [ ] **Step 3: Run the reducer test.** Expected: PASS.

- [ ] **Step 4: Rewrite `hooks/useGame.js`.**
```js
import { useCallback, useEffect, useMemo, useReducer, useState } from 'react';
import { useMutation, useSubscription } from '@apollo/client/react';
import { JOIN_GAME, PLAYER_ACTION, LEAVE_GAME, SIT_OUT, SIT_IN, START_HAND, SEND_CHAT } from '../graphql/mutations';
import { GAME_UPDATED, PLAYER_UPDATED } from '../graphql/subscriptions';
import { heroIndex as findHero, initialTableState, tableReducer } from '../lib/gameReducer';

const BETTING = ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'];

/**
 * Everything the table screen needs: join, both subscriptions folded through tableReducer, the
 * hero's seat and turn, and the mutations. The acting player is always resolved server-side from
 * the auth token; nothing here sends a player id.
 */
export function useGame(gameId, username) {
  const [state, dispatch] = useReducer(tableReducer, initialTableState);
  const [status, setStatus] = useState('joining');
  const [error, setError] = useState(null);

  const [joinGame] = useMutation(JOIN_GAME);
  const [playerAction] = useMutation(PLAYER_ACTION);
  const [leaveGame] = useMutation(LEAVE_GAME);
  const [sitOutM] = useMutation(SIT_OUT);
  const [sitInM] = useMutation(SIT_IN);
  const [startHandM] = useMutation(START_HAND);
  const [sendChatM] = useMutation(SEND_CHAT);

  useEffect(() => {
    if (!gameId || !username) return;
    let cancelled = false;
    setStatus('joining');
    joinGame({ variables: { gameId } })
      .then(({ data }) => {
        if (cancelled) return;
        const g = data?.joinGame;
        if (!g || findHero(g, username) === -1) throw new Error('Could not take a seat at this table.');
        dispatch({ type: 'joined', game: g });
        setStatus('ready');
      })
      .catch((e) => { if (!cancelled) { setError(e.message || 'Failed to join the table.'); setStatus('error'); } });
    return () => { cancelled = true; };
  }, [gameId, username, joinGame]);

  const ready = status === 'ready';
  useSubscription(GAME_UPDATED, {
    variables: { gameId },
    skip: !ready,
    onData: ({ data }) => data.data?.gameUpdated && dispatch({ type: 'update', update: data.data.gameUpdated }),
  });
  useSubscription(PLAYER_UPDATED, {
    variables: { gameId },
    skip: !ready,
    onData: ({ data }) => data.data?.playerUpdated && dispatch({ type: 'update', update: data.data.playerUpdated }),
  });

  const heroIndex = findHero(state.game, username);
  const hero = heroIndex >= 0 ? state.game.players[heroIndex] : null;
  const isMyTurn = !!(hero && BETTING.includes(state.game.status)
    && state.game.currentPlayerIndex === heroIndex && !hero.hasFolded && !hero.isSittingOut);

  const vars = { variables: { gameId } };
  const actions = useMemo(() => ({
    bet: (amount) => playerAction({ variables: { gameId, input: { actionType: 'BET', amount } } }),
    check: () => playerAction({ variables: { gameId, input: { actionType: 'CHECK' } } }),
    fold: () => playerAction({ variables: { gameId, input: { actionType: 'FOLD' } } }),
    leave: () => leaveGame(vars),
    sitOut: () => sitOutM(vars),
    sitIn: () => sitInM(vars),
    startHand: () => startHandM(vars),
    sendChat: (message) => sendChatM({ variables: { gameId, message } }),
  }), [gameId]); // eslint-disable-line react-hooks/exhaustive-deps

  const clearShowdown = useCallback(() => dispatch({ type: 'update', update: { type: 'SHOWDOWN_SEEN' } }), []);

  return { status, error, ...state, clearShowdown, heroIndex, hero, isMyTurn, actions };
}
```
Then add a `SHOWDOWN_SEEN` case to `tableReducer`: `case 'SHOWDOWN_SEEN': return { ...state, showdown: null };`, with a test: `expect(tableReducer({ ...joined, showdown: {} }, update('SHOWDOWN_SEEN')).showdown).toBeNull();`.

Before relying on `onData`, check it exists in Apollo 4.1.6: `grep -n "onData" node_modules/@apollo/client/react/hooks/useSubscription.d.ts`. If it doesn't, fall back to `const { data } = useSubscription(...)` plus `useEffect(() => data && dispatch(...), [data])`.

- [ ] **Step 5: Run the checks.** Run `npx vitest run` and `npm run build`; both must pass. Vite does not type-check, so the build still succeeds even though the old `PokerTable.jsx` now calls `useGame` with the wrong shape: the table page is broken at runtime until Task 13 replaces it. That is acceptable on this branch (nothing deploys from it), but do not open a preview between Tasks 10 and 13.

- [ ] **Step 6: Commit.**
```bash
git add frontend
git commit -m "feat(table): pure table reducer; useGame owns join, subscriptions, turn and actions

Fixes chat never arriving: the backend sends CHAT_MESSAGE, the page listened for CHAT.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Seat layout + table scene restyle

**Files:**
- Create: `frontend/src/lib/seatLayout.js`
- Modify: `frontend/src/components/poker/OvalTable.jsx` (rename the export to `TableScene`, file `TableScene.jsx`), `PlayerSeat.jsx`, `PokerCard.jsx`, `PotDisplay.jsx`, `CommunityCards.jsx`, `PokerChip.jsx`
- Test: `frontend/src/lib/__tests__/seatLayout.test.js`

**Interfaces:**
- Consumes: `game`, `heroIndex`, `hand` from `useGame` (Task 10).
- Produces:
  - `seatLayout(seatCount, heroIndex, orientation = 'portrait') -> Array<{ left: number, top: number }>` (percentages, 0-100), indexed by the **actual** player index, hero at `{ left: 50, top: ~92 }`.
  - `<TableScene game heroIndex hand highlight={Set<string>|null} orientation />`, where `highlight` holds card keys `"RANK-SUIT"` to lift during showdown (Task 14).
  - `<PokerCard card faceDown small highlighted dimmed />`.

- [ ] **Step 1: Write the failing test.**
```js
import { describe, expect, it } from 'vitest';
import { seatLayout } from '../seatLayout';

describe('seatLayout', () => {
  for (let n = 2; n <= 9; n++) {
    for (let hero = 0; hero < n; hero++) {
      it(`puts the hero bottom-center with ${n} seats (hero ${hero})`, () => {
        const seats = seatLayout(n, hero);
        expect(seats).toHaveLength(n);
        expect(seats[hero].left).toBeCloseTo(50, 5);
        const maxTop = Math.max(...seats.map((s) => s.top));
        expect(seats[hero].top).toBeCloseTo(maxTop, 5);
      });
    }
  }

  it('places seats clockwise from the hero and keeps them inside the box', () => {
    const seats = seatLayout(6, 2);
    for (const s of seats) {
      expect(s.left).toBeGreaterThanOrEqual(0);
      expect(s.left).toBeLessThanOrEqual(100);
      expect(s.top).toBeGreaterThanOrEqual(0);
      expect(s.top).toBeLessThanOrEqual(100);
    }
    // next seat clockwise from bottom-center is to the left on screen (dealing order)
    expect(seats[3].left).toBeLessThan(50);
  });

  it('with no hero seat, index 0 takes the bottom', () => {
    expect(seatLayout(4, -1)[0].left).toBeCloseTo(50, 5);
  });

  it('is empty for no seats', () => {
    expect(seatLayout(0, -1)).toEqual([]);
  });
});
```
Run it. Expected: FAIL.

- [ ] **Step 2: Implement.**
```js
// Ellipse radii in % of the scene box. Portrait (phones) is a tall oval, landscape a wide one.
const RADII = { portrait: { rx: 40, ry: 42 }, landscape: { rx: 44, ry: 38 } };

/**
 * Screen position of every seat, indexed by actual player index, rotated so the hero sits
 * bottom-center and the rest follow clockwise (the order the action moves).
 */
export function seatLayout(seatCount, heroIndex, orientation = 'portrait') {
  const { rx, ry } = RADII[orientation] || RADII.portrait;
  const anchor = heroIndex >= 0 ? heroIndex : 0;
  return Array.from({ length: seatCount }, (_, i) => {
    const step = (i - anchor + seatCount) % seatCount;
    // Start at the bottom (90° in screen coordinates, y down) and go clockwise on screen.
    const angle = Math.PI / 2 + (step * 2 * Math.PI) / seatCount;
    return { left: 50 + rx * Math.cos(angle), top: 50 + ry * Math.sin(angle) };
  });
}
```
Check the clockwise assertion against the math: step 1 of 6 gives an angle of 150°, cos < 0, so left < 50. It passes. Run the test. Expected: PASS.

- [ ] **Step 3: Restyle into `TableScene.jsx`** (`git mv OvalTable.jsx TableScene.jsx`).
  - Box: `relative w-full aspect-[3/4] lg:aspect-[16/10]`.
  - Floor: an absolute `inset-[8%] rounded-[50%]` element with `background: radial-gradient(ellipse at center, color-mix(in oklab, var(--color-neon-cyan) 8%, var(--color-table-floor)) 0%, var(--color-table-floor) 70%)` and `border: 2px solid var(--color-table-rail)`, `box-shadow: var(--glow-md), inset 0 0 60px rgb(0 0 0 / 0.6)`.
  - Center: `PotDisplay` (magenta, `font-mono tabular`) and `CommunityCards`.
  - Seats come from `seatLayout(players.length, heroIndex, orientation)`. Empty seats are removed: the board shows only the players who are seated.
  - Delete the `WinnerOverlay` usage.
  - `orientation` comes from `useMediaQuery('(min-width: 1024px)') ? 'landscape' : 'portrait'` in the page.
  - Pass `hand` to the hero seat only.

- [ ] **Step 4: Restyle `PlayerSeat`.** Take the props `{ player, isHero, isTurn, isDealer, blind, hand, bet, highlight }`.
  - Avatar: 48px, `rounded-full bg-surface-elevated border`. Border is `border-neon-cyan glow-cyan` for the hero, `border-border-strong` for others. Initials come from `player.name` (never `username`).
  - Name plate: `player.name`, truncated, `text-xs`; chips in `font-mono tabular text-xs text-text-muted` via `formatChips`.
  - States: folded is `opacity-40 grayscale`; all-in is a magenta `ALL IN` tag (`text-xs`); sitting out is a grey `AWAY` tag.
  - Dealer `D` and `SB`/`BB` markers are 20px circles with `text-xs`.
  - Bot badge uses `text-xs`.
  - The bet chip sits toward the center.
  - Hero's cards: two `PokerCard`s, `highlighted`/`dimmed` driven by `highlight`.
  - Delete `CircuitPattern` and all hardcoded hex.
  - Leave a slot for `TurnRing` (Task 12): wrap the avatar in `relative` so the ring can absolutely overlay it.

- [ ] **Step 5: Restyle `PokerCard`.**
  - Faces: `bg-[#f5f6ff]` is not allowed, so use `bg-text` with `text-background` for dark suits and `text-danger` for red suits. Face-down: `bg-surface-overlay` with a cyan hairline border and a subtle diagonal-stripe `background-image` built with `repeating-linear-gradient` from tokens.
  - Pip text uses `text-xs` (small cards) and `text-sm` (regular cards).
  - `highlighted` adds `-translate-y-2 glow-magenta ring-2 ring-neon-magenta`; `dimmed` adds `opacity-40`.
  - Keep the flip animation (it's disabled by the reduced-motion rule).

- [ ] **Step 6: Run the checks.** Run `npx vitest run src/lib/__tests__/seatLayout.test.js` and grep the poker components for hex and micro sizes (expected none in the files touched).

- [ ] **Step 7: Commit.**
```bash
git add -A frontend
git commit -m "feat(table): hero-relative seat layout and neon table scene

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Turn ring

**Files:**
- Create: `frontend/src/lib/turnProgress.js`
- Create: `frontend/src/components/poker/TurnRing.jsx`
- Modify: `frontend/src/components/poker/PlayerSeat.jsx` (render the ring when `isTurn`)
- Test: `frontend/src/lib/__tests__/turnProgress.test.js`

**Interfaces:**
- Consumes: `game.currentPlayerActionDeadline` (ISO string or null) and `game.playerActionTimeoutSeconds` (int).
- Produces: `turnProgress(deadlineIso, timeoutSeconds, nowMs) -> { remainingMs, fraction, urgent }`, where `fraction` is in [0,1] (1 = full time left) and `urgent` is true at 5000ms or less. `<TurnRing deadline timeoutSeconds size={56} onHeroTurn? />`.

- [ ] **Step 1: Write the failing test.**
```js
import { describe, expect, it } from 'vitest';
import { turnProgress } from '../turnProgress';

const now = Date.parse('2026-09-25T10:00:00Z');

describe('turnProgress', () => {
  it('is full at the start of a turn', () => {
    const p = turnProgress('2026-09-25T10:00:30Z', 30, now);
    expect(p.remainingMs).toBe(30000);
    expect(p.fraction).toBe(1);
    expect(p.urgent).toBe(false);
  });

  it('is urgent at five seconds left', () => {
    const p = turnProgress('2026-09-25T10:00:05Z', 30, now);
    expect(p.fraction).toBeCloseTo(5 / 30);
    expect(p.urgent).toBe(true);
  });

  it('clamps past the deadline and beyond the timeout', () => {
    expect(turnProgress('2026-09-25T09:59:50Z', 30, now)).toEqual({ remainingMs: 0, fraction: 0, urgent: true });
    expect(turnProgress('2026-09-25T10:01:00Z', 30, now).fraction).toBe(1);
  });

  it('has no progress without a deadline', () => {
    expect(turnProgress(null, 30, now)).toEqual({ remainingMs: null, fraction: 1, urgent: false });
  });
});
```
Run it. Expected: FAIL.

- [ ] **Step 2: Implement `lib/turnProgress.js`.**
```js
const URGENT_MS = 5000;

export function turnProgress(deadlineIso, timeoutSeconds, nowMs = Date.now()) {
  if (!deadlineIso || !timeoutSeconds) return { remainingMs: null, fraction: 1, urgent: false };
  const remainingMs = Math.max(0, Date.parse(deadlineIso) - nowMs);
  const fraction = Math.min(1, remainingMs / (timeoutSeconds * 1000));
  return { remainingMs, fraction, urgent: remainingMs <= URGENT_MS };
}
```
Run the test. Expected: PASS.

- [ ] **Step 3: Implement `TurnRing.jsx`.**
```jsx
import React, { useEffect, useRef, useState } from 'react';
import { turnProgress } from '@/lib/turnProgress';

/** Countdown ring around the acting seat. Re-renders ~4x a second; stops when there is no deadline. */
export default function TurnRing({ deadline, timeoutSeconds, size = 56, isHero = false }) {
  const [now, setNow] = useState(() => Date.now());
  const buzzed = useRef(null);

  useEffect(() => {
    if (!deadline) return;
    const id = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(id);
  }, [deadline]);

  useEffect(() => {
    if (isHero && deadline && buzzed.current !== deadline) {
      buzzed.current = deadline;
      navigator.vibrate?.(60);
    }
  }, [isHero, deadline]);

  const { fraction, urgent, remainingMs } = turnProgress(deadline, timeoutSeconds, now);
  const r = size / 2 - 3;
  const c = 2 * Math.PI * r;
  const color = urgent ? 'var(--color-warning)' : 'var(--color-neon-cyan)';

  return (
    <svg width={size} height={size} className="absolute -inset-1 -rotate-90 pointer-events-none" aria-hidden="true">
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--color-border-strong)" strokeWidth="3" />
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth="3" strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={c * (1 - fraction)}
        style={{ filter: `drop-shadow(0 0 4px ${color})`, transition: 'stroke-dashoffset 250ms linear' }} />
      {remainingMs != null && urgent && <title>{Math.ceil(remainingMs / 1000)}s left</title>}
    </svg>
  );
}
```
In `PlayerSeat`, render `{isTurn && <TurnRing deadline={deadline} timeoutSeconds={timeoutSeconds} isHero={isHero} />}` inside the avatar wrapper. `TableScene` passes `deadline={game.currentPlayerActionDeadline}` and `timeoutSeconds={game.playerActionTimeoutSeconds}` through.

- [ ] **Step 4: Run the checks.** Run `npx vitest run` and `npm run build`.

- [ ] **Step 5: Commit.**
```bash
git add frontend
git commit -m "feat(table): countdown turn ring from the server action deadline

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 13: Action bar, top bar, chat drawer, thin PokerTable page

**Files:**
- Create: `frontend/src/components/poker/ActionBar.jsx` (from `BettingControls.jsx`, then delete that file)
- Create: `frontend/src/components/poker/TableTopBar.jsx`
- Create: `frontend/src/components/poker/ChatDrawer.jsx`
- Rewrite: `frontend/src/pages/PokerTable.jsx`
- Modify: `frontend/src/components/poker/AddBotButton.jsx` (styles only)
- Test: `frontend/src/components/poker/__tests__/ActionBar.test.jsx`

**Interfaces:**
- Consumes: `useGame` (Task 10), `useConnectionStatus` + `ConnectionPill` (Task 9), `TableScene` (Task 11), `BottomSheet`, `useMediaQuery`.
- Produces:
  - `<ActionBar game hero heroIndex isMyTurn online actions />`: disabled when `!online || !isMyTurn`; pre-hand (`game.status === 'WAITING'`) shows Start hand + Add bot; off-turn seated shows Sit out / Sit in.
  - `<TableTopBar game online status onLeave onOpenChat unread />`.
  - `<ChatDrawer open onClose messages onSend docked />`.

- [ ] **Step 1: Write the failing test.**
```jsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ActionBar from '../ActionBar';

vi.mock('../AddBotButton', () => ({ default: () => <button type="button">Add bot</button> }));

const hero = { id: 'p1', chips: 1000, hasFolded: false, isSittingOut: false };
const game = { id: 'g1', status: 'FLOP_BETTING', currentBet: 20, pot: 60, bigBlindAmount: 20,
  currentBettingRound: { bets: { p1: 0 } }, players: [hero] };
const actions = () => ({ bet: vi.fn(), check: vi.fn(), fold: vi.fn(), sitOut: vi.fn(), sitIn: vi.fn(), startHand: vi.fn() });

describe('ActionBar', () => {
  it('lets the hero act on their turn', async () => {
    const a = actions();
    render(<ActionBar game={game} hero={hero} heroIndex={0} isMyTurn online actions={a} />);
    await userEvent.click(screen.getByRole('button', { name: /fold/i }));
    expect(a.fold).toHaveBeenCalled();
    expect(screen.getByRole('button', { name: /call 20/i })).toBeEnabled();
  });

  it('disables betting while offline', () => {
    render(<ActionBar game={game} hero={hero} heroIndex={0} isMyTurn online={false} actions={actions()} />);
    expect(screen.getByRole('button', { name: /fold/i })).toBeDisabled();
  });

  it('shows no betting buttons off-turn, only sit out', () => {
    render(<ActionBar game={game} hero={hero} heroIndex={0} isMyTurn={false} online actions={actions()} />);
    expect(screen.queryByRole('button', { name: /fold/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sit out/i })).toBeInTheDocument();
  });

  it('offers start hand and add bot while waiting', () => {
    render(<ActionBar game={{ ...game, status: 'WAITING', players: [hero, { id: 'p2' }] }} hero={hero} heroIndex={0}
      isMyTurn={false} online actions={actions()} />);
    expect(screen.getByRole('button', { name: /start hand/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /add bot/i })).toBeInTheDocument();
  });
});
```
Run it. Expected: FAIL.

- [ ] **Step 2: Implement `ActionBar.jsx`.** Start from `BettingControls.jsx`'s math (`callAmount`, `minRaise`, `maxRaise`, snap points, slider) and change:
  - Props become `{ game, hero, heroIndex, isMyTurn, online, actions }`. `myChips = hero.chips`, `playerCurrentBet = game.currentBettingRound?.bets?.[hero.id] ?? 0`.
  - Container: `fixed lg:static bottom-0 inset-x-0 z-30 p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] bg-surface/95 backdrop-blur border-t border-border`.
  - `const disabled = !online;`
  - When `game.status === 'WAITING'`: a row with `<Button onClick={actions.startHand} disabled={disabled || game.players.length < 2}>Start hand</Button>` and `<AddBotButton gameId={game.id} />`.
  - When seated and not your turn: `hero.isSittingOut ? Sit in (success) : Sit out (ghost)`.
  - On your turn: a grid of 3 buttons, each `h-14` (above the 48px minimum), all with `disabled={disabled}`:
    - Fold: `variant="danger"`, `aria-label="Fold"`.
    - Check / Call: `variant="surface"`; label `Check` or `Call {formatChips(callAmount)}`, and an `aria-label` with the same text.
    - Raise: `variant="default"` with `glow-cyan`. It opens the slider panel: presets ½ pot / pot / all-in, then a confirm button "Raise to {formatChips(raiseAmount)}" that calls `actions.bet(raiseAmount)`. Call uses `actions.bet(callAmount)`, check uses `actions.check()`, fold uses `actions.fold()`.
  - Keyboard shortcuts at `lg:` via a `useEffect` keydown listener, active only when `isMyTurn && online` and not while focus is in an input or textarea: `f` fold, `c` check/call, `r` open raise.
  - Every action promise gets `.catch((e) => toast.error(e.message))`.
  - Delete `BettingControls.jsx`.

- [ ] **Step 3: `TableTopBar.jsx`.** Layout: `h-14 flex items-center gap-2 px-3 border-b border-border bg-surface/80 backdrop-blur`.
  - Leave button (`icon` ghost, `LogOut`, `aria-label="Leave table"`). It opens a small confirm `Dialog`: "Leave the table? Your chips at this table are forfeited." with Leave (danger) and Stay buttons. Confirming calls `onLeave`.
  - Center: `Table #{id.slice(-4)}` and blinds `formatBlinds(...)` in `font-mono`.
  - Right: `<ConnectionPill status={status} />`, then the chat button (`MessageSquare`, `aria-label="Open chat"`) with an unread dot (`size-2 rounded-full bg-neon-magenta`) when `unread > 0`. Hide the chat button at `lg:`, where chat is docked.

- [ ] **Step 4: `ChatDrawer.jsx`.** Move `ChatPanel` and `RankingsPanel` (with `HAND_RANKINGS`) out of `PokerTable.jsx` into this file.
  - Render `Tabs` (Chat / Hands).
  - `docked` true: render inline as an `aside` (`w-80 border-l border-border bg-surface flex flex-col`). Otherwise, wrap it in `<BottomSheet open onClose title="Table chat">`.
  - Chat item key: `${m.timestamp}-${m.senderId}`. The sender name is `text-neon-cyan` when it's the hero.
  - Input: 44px high, sends on Enter, max 500 characters (the backend limit).

- [ ] **Step 5: Rewrite `pages/PokerTable.jsx`** so it only does layout:
```jsx
import React, { useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { AuthContext } from '@/context/AuthContext';
import { useGame } from '@/hooks/useGame';
import { useConnectionStatus } from '@/hooks/useConnectionStatus';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import TableScene from '@/components/poker/TableScene';
import ActionBar from '@/components/poker/ActionBar';
import TableTopBar from '@/components/poker/TableTopBar';
import ChatDrawer from '@/components/poker/ChatDrawer';
import { Skeleton } from '@/components/ui/skeleton';

export default function PokerTable() {
  const { gameId } = useParams();
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const desktop = useMediaQuery('(min-width: 1024px)');
  const connection = useConnectionStatus();
  const t = useGame(gameId, user?.username);
  const [chatOpen, setChatOpen] = useState(false);
  const [seenChat, setSeenChat] = useState(0);

  useEffect(() => { if (chatOpen || desktop) setSeenChat(t.chat.length); }, [chatOpen, desktop, t.chat.length]);
  useEffect(() => {
    if (t.status === 'error') { toast.error(t.error); navigate('/lobby', { replace: true }); }
  }, [t.status]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (t.status === 'ready' && t.heroIndex === -1) { toast.info('You are no longer seated at this table.'); navigate('/lobby', { replace: true }); }
  }, [t.status, t.heroIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  if (t.status !== 'ready') {
    return (
      <div className="min-h-dvh flex flex-col items-center justify-center gap-6 p-6" aria-busy="true">
        <Skeleton className="w-full max-w-sm aspect-[3/4] rounded-[50%]" />
        <p className="text-text-muted text-sm">Taking a seat…</p>
      </div>
    );
  }

  const leave = async () => {
    try { await t.actions.leave(); } catch (e) { toast.error(e.message); }
    navigate('/lobby');
  };

  return (
    <div className="h-dvh flex flex-col bg-background overflow-hidden">
      <TableTopBar game={t.game} status={connection} onLeave={leave}
        onOpenChat={() => setChatOpen(true)} unread={t.chat.length - seenChat} />
      <div className="flex-1 flex min-h-0">
        <main className="flex-1 flex flex-col min-h-0">
          <div className="flex-1 flex items-center justify-center p-4 pb-40 lg:pb-4 min-h-0">
            <div className="w-full max-w-md lg:max-w-4xl">
              <TableScene game={t.game} heroIndex={t.heroIndex} hand={t.hand}
                orientation={desktop ? 'landscape' : 'portrait'} />
            </div>
          </div>
          <ActionBar game={t.game} hero={t.hero} heroIndex={t.heroIndex} isMyTurn={t.isMyTurn}
            online={connection === 'online'} actions={t.actions} />
        </main>
        {desktop && <ChatDrawer docked messages={t.chat} heroId={t.hero?.id} onSend={t.actions.sendChat} />}
      </div>
      {!desktop && <ChatDrawer open={chatOpen} onClose={() => setChatOpen(false)} messages={t.chat}
        heroId={t.hero?.id} onSend={t.actions.sendChat} />}
      <div aria-live="polite" className="sr-only">{t.isMyTurn ? 'Your turn' : ''}</div>
    </div>
  );
}
```
Delete everything else that was in the old file (`LeftSidebar`, `RightSidebar`, `MobileActionBar`, `StartHandOverlay`, `GAME_STATUS`).

- [ ] **Step 6: Restyle `AddBotButton`.** Tokens only, sm outline button.

- [ ] **Step 7: Run the checks.** Run `npx vitest run` and `npm run build`. Both must be green now.

- [ ] **Step 8: Test the real table locally.** Start a backend from this worktree against a throwaway DB name:
```bash
cd /home/ubuntu/projects/Betrix-wt-redesign && cp /home/ubuntu/runners/actions-runner-betrix/betrix.env .env
# edit .env: SPRING_DATA_MONGODB_URI -> same cluster, database name betrix-preview;
#            GOOGLE_CLIENT_ID=<value if Kush has provided it, else any placeholder>;
#            APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
cd backend && set -a && . ../.env && set +a && SERVER_PORT=8084 ./gradlew bootRun
```
Run it in the background. Then start the frontend with `VITE_SERVER_HOST=http://localhost VITE_SERVER_PORT=8084 npm run dev -- --port 3000`. With `example-skills:webapp-testing`:
  1. Play as guest.
  2. Create a table.
  3. Add a bot.
  4. Start a hand.
  5. Fold / call through to the end.

Screenshot at 375 and 1280. Confirm:
  - The turn ring counts down.
  - Chat from the page shows up.
  - Killing the backend shows "Reconnecting…".

`.env` is gitignored; confirm with `git status` that it's not staged.

- [ ] **Step 9: Commit.**
```bash
git add -A frontend
git commit -m "feat(table): thin table page with top bar, bottom action bar and chat drawer

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 14: Showdown reveal

**Files:**
- Create: `frontend/src/lib/showdown.js`
- Create: `frontend/src/components/poker/ShowdownReveal.jsx`
- Delete: `frontend/src/components/poker/WinnerOverlay.jsx`
- Modify: `frontend/src/components/poker/TableScene.jsx`, `PlayerSeat.jsx`, `frontend/src/pages/PokerTable.jsx`
- Test: `frontend/src/lib/__tests__/showdown.test.js`

**Interfaces:**
- Consumes: `showdown` + `clearShowdown` from `useGame` (Task 10); `highlight` props on `TableScene`/`PlayerSeat`/`PokerCard` (Task 11).
- Produces:
  - `cardKey(card) -> "RANK-SUIT"`.
  - `showdownHighlight(showdown) -> Set<string> | null` (null when there's no real showdown).
  - `HAND_RANK_LABEL` map (e.g. `FULL_HOUSE` → `"Full House"`).
  - `winnerIds(showdown) -> Set<string>`.
  - `<ShowdownReveal showdown onDone />`: a banner that auto-dismisses after 3000ms or on tap, then calls `onDone`.

- [ ] **Step 1: Write the failing test.**
```js
import { describe, expect, it } from 'vitest';
import { cardKey, HAND_RANK_LABEL, showdownHighlight, winnerIds } from '../showdown';

const flush = [
  { rank: 'ACE', suit: 'HEARTS' }, { rank: 'TEN', suit: 'HEARTS' }, { rank: 'EIGHT', suit: 'HEARTS' },
  { rank: 'FIVE', suit: 'HEARTS' }, { rank: 'TWO', suit: 'HEARTS' },
];

describe('showdown helpers', () => {
  it('highlights exactly the best-hand cards', () => {
    const set = showdownHighlight({ bestHand: { rank: 'FLUSH', highCards: flush }, winners: [] });
    expect([...set].sort()).toEqual(flush.map(cardKey).sort());
    expect(set.has('KING-SPADES')).toBe(false);
  });

  it('highlights nothing when the hand ended without a showdown', () => {
    expect(showdownHighlight({ bestHand: null, winners: [{ id: 'p1' }] })).toBeNull();
    expect(showdownHighlight(null)).toBeNull();
  });

  it('labels every hand rank', () => {
    for (const r of ['HIGH_CARD', 'ONE_PAIR', 'TWO_PAIR', 'THREE_OF_A_KIND', 'STRAIGHT', 'FLUSH',
      'FULL_HOUSE', 'FOUR_OF_A_KIND', 'STRAIGHT_FLUSH', 'ROYAL_FLUSH']) {
      expect(HAND_RANK_LABEL[r]).toMatch(/^[A-Z]/);
    }
  });

  it('collects winner ids', () => {
    expect([...winnerIds({ winners: [{ id: 'p1' }, { id: 'p3' }] })]).toEqual(['p1', 'p3']);
    expect(winnerIds(null).size).toBe(0);
  });
});
```
Run it. Expected: FAIL.

- [ ] **Step 2: Implement `lib/showdown.js`.**
```js
export const cardKey = (c) => `${c.rank}-${c.suit}`;

export const HAND_RANK_LABEL = {
  HIGH_CARD: 'High Card', ONE_PAIR: 'Pair', TWO_PAIR: 'Two Pair', THREE_OF_A_KIND: 'Three of a Kind',
  STRAIGHT: 'Straight', FLUSH: 'Flush', FULL_HOUSE: 'Full House', FOUR_OF_A_KIND: 'Four of a Kind',
  STRAIGHT_FLUSH: 'Straight Flush', ROYAL_FLUSH: 'Royal Flush',
};

/** The cards to lift. Null when everyone else folded: the backend sends no bestHand then. */
export function showdownHighlight(showdown) {
  const cards = showdown?.bestHand?.highCards;
  return cards?.length ? new Set(cards.map(cardKey)) : null;
}

export function winnerIds(showdown) {
  return new Set((showdown?.winners || []).map((w) => w.id));
}
```
Run the test. Expected: PASS.

- [ ] **Step 3: `ShowdownReveal.jsx`.** This is an absolutely positioned banner over the table center (inside `TableScene`'s box), not a modal. Use `framer-motion` scale/opacity in.
  - Content: `{winnerNames} wins {formatChips(total)}` (total = sum of `winners[].lastWinAmount`), plus the hand name `HAND_RANK_LABEL[bestHand.rank]` in `font-display text-2xl text-neon-magenta` with a magenta text-shadow. Without `bestHand`, show "Everyone else folded".
  - The whole banner is a `<button type="button" onClick={onDone} aria-label="Dismiss result">`. A `useEffect` calls `setTimeout(onDone, 3000)` keyed on `showdown`.
  - Also render `<div aria-live="polite" className="sr-only">` with the same sentence.

- [ ] **Step 4: Wire it up.**
  - `TableScene` gets the props `showdown` and `onShowdownDone`. It computes `highlight = showdownHighlight(showdown)` and `winners = winnerIds(showdown)`.
  - Winner seats get `glow-magenta` and `border-neon-magenta`. With `highlight` set, community cards and the hero's cards in the set get `highlighted`, the rest `dimmed`.
  - Opponents' revealed cards at showdown come from `showdown.game.players[i].hand`. Pass that to each seat while `showdown` is set, so opponents' cards flip face-up.
  - The seat chip counts animate to their new values (framer-motion `animate` on a number, or a key change with a pulse).
  - Queued state: `useGame` already keeps `showdown` until `GAME_STARTED`/`ROUND_STARTED`. To hold the next hand for the reveal, `PokerTable` keeps `const [revealing, setRevealing] = useState(false)`, set to true when `t.showdown` changes to non-null. It renders `TableScene` with `t.showdown` while `revealing`, and `onShowdownDone={() => { setRevealing(false); t.clearShowdown(); }}`. Because `tableReducer` still applies new `GAME_STARTED` state underneath, the reveal holds only the *overlay*, never the betting state. That is intentional, so a fast bot table can't stall the hero's action.
  - Delete `WinnerOverlay.jsx`.

- [ ] **Step 5: Run the checks.** Run `npx vitest run` and `npm run build`. Then repeat Task 13 Step 8's local run to a showdown (call down with a bot) and screenshot the reveal at 375 and 1280.

- [ ] **Step 6: Commit.**
```bash
git add -A frontend
git commit -m "feat(table): showdown reveal lifts the winning five and names the hand

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 15: Polish, audit, finish review, DESIGN.md

**Files:**
- Modify: any frontend files the reviews flag
- Create: `DESIGN.md` (+ its sidecar, if impeccable-documenter emits one)
- Create: `docs/redesign/after/*.png`

- [ ] **Step 1: Screenshots.** Invoke `example-skills:webapp-testing`. With the Task 13 local stack running, capture every screen at 375x812, 768x1024 and 1280x800 into `docs/redesign/after/`: Home, Lobby (empty + with tables), Create-table sheet, Profile (guest), 404, Table (waiting, your turn with raise open, reconnecting, showdown).

- [ ] **Step 2: Polish and audit.** Invoke `impeccable:impeccable` and run `polish`, then `audit`, per screen against `PRODUCT.md` and the spec. Fix the material findings.

- [ ] **Step 3: Global-constraint sweep.**
```bash
cd frontend/src
grep -rnE "#[0-9a-fA-F]{3,8}\b" --include=*.jsx .           # expect none
grep -rnE "text-\[(8|9|10|11)px\]" .                         # expect none
grep -rn '\$' --include=*.jsx . | grep -v '\${'              # expect no currency signs
grep -rnE "\bgold\b|felt|font-serif" .                       # expect none
```

- [ ] **Step 4: Finish review.** Dispatch the `impeccable:impeccable-finish-reviewer` agent with the spec path, `PRODUCT.md` and the after screenshots. Fix what it returns as material.

- [ ] **Step 5: DESIGN.md.** Dispatch `impeccable:impeccable-documenter` to write `DESIGN.md` from the shipped code.

- [ ] **Step 6: Final checks.** Run `npx vitest run`, `npm run build` (frontend), and `./gradlew test` (backend). All must be green.

- [ ] **Step 7: Commit and push.**
```bash
git add -A
git commit -m "chore(ui): polish and audit fixes; document the design system

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push origin redesign
```

---

### Task 16: Preview deployment

**Prerequisite (Kush):** a Google OAuth 2.0 **Web application** client from Google Cloud Console → APIs & Services → Credentials. Authorized JavaScript origins:
- `https://betrix-b3c24.web.app`
- `https://betrix-b3c24.firebaseapp.com`
- `http://localhost:3000`
- the preview channel URL (added after Step 4 prints it)

No redirect URIs are needed. Ask Kush for the client ID and the admin email(s). If they aren't available yet, do Steps 1-3 with a placeholder client ID, then stop and report back.

**Files:** none in the repo, VM infrastructure only. The preview Nginx site is temporary.

- [ ] **Step 1: Preview backend container** (separate from the live `betrix-backend`, and its own database):
```bash
cd /home/ubuntu/projects/Betrix-wt-redesign
cp /home/ubuntu/runners/actions-runner-betrix/betrix.env /home/ubuntu/secrets/betrix-preview.env && chmod 600 /home/ubuntu/secrets/betrix-preview.env
# edit betrix-preview.env: SPRING_DATA_MONGODB_URI database name -> betrix-preview;
#   GOOGLE_CLIENT_ID=<from Kush>; APP_ADMIN_EMAILS=<from Kush>;
#   APP_CORS_ALLOWED_ORIGINS=<preview channel origin, added after Step 4>,http://localhost:3000
docker build -t betrix-backend:preview backend
docker run -d --name betrix-preview --restart unless-stopped -p 127.0.0.1:8084:8080 \
  -e SERVER_PORT=8080 --env-file /home/ubuntu/secrets/betrix-preview.env betrix-backend:preview
curl -s http://127.0.0.1:8084/actuator/health
```
Expected: `{"status":"UP"}`. Before picking 8084, check it's free with `ss -tlnp | grep 8084`.

- [ ] **Step 2: Nginx + TLS.** Copy `/etc/nginx/sites-available/betrix.conf` to `betrix-preview.conf`. Change `server_name` to `betrix-preview.161.118.167.148.nip.io` and every `proxy_pass` port 8083 to 8084. Remove any certbot-managed `listen 443`/`ssl_*` lines so it starts HTTP-only. Keep the `/graphql` WebSocket location. Then:
```bash
sudo ln -sf /etc/nginx/sites-available/betrix-preview.conf /etc/nginx/sites-enabled/betrix-preview.conf
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d betrix-preview.161.118.167.148.nip.io --non-interactive --agree-tos -m siddharth.rajagopalan01@gmail.com --redirect
curl -s https://betrix-preview.161.118.167.148.nip.io/actuator/health
```

- [ ] **Step 3: Verify the WebSocket for real** (per CLAUDE.md): use Python `websockets` to connect to `wss://betrix-preview.161.118.167.148.nip.io/graphql` with subprotocol `graphql-transport-ws`, send `{"type":"connection_init","payload":{}}`, and expect `connection_ack`.

- [ ] **Step 4: Firebase preview channel.**
```bash
cd /home/ubuntu/projects/Betrix-wt-redesign/frontend
VITE_SERVER_HOST=https://betrix-preview.161.118.167.148.nip.io VITE_SERVER_PORT= VITE_GOOGLE_CLIENT_ID=<client id> npm run build
firebase hosting:channel:deploy redesign --expires 30d --project betrix-b3c24
```
If `firebase` isn't logged in, ask Kush to run `! firebase login --no-localhost`. Note the printed channel URL. Add its origin to `APP_CORS_ALLOWED_ORIGINS` in `betrix-preview.env` and recreate the container (`docker rm -f betrix-preview` + Step 1's `docker run`). Ask Kush to add the origin to the OAuth client.

- [ ] **Step 5: CORS check with the real origin.**
```bash
curl -s -i -X OPTIONS https://betrix-preview.161.118.167.148.nip.io/graphql \
  -H "Origin: <channel origin>" -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type" | grep -i "access-control\|HTTP/"
```
Expected: 200 and `Access-Control-Allow-Origin: <channel origin>`.

- [ ] **Step 6: End-to-end on the preview.**
  - Google sign-in: Kush does it himself, since it needs a real Google account.
  - Guest: join a table, add a bot, and play through a showdown. Check it at 375 width on an actual phone if possible.

- [ ] **Step 7: Report to Kush.** Send the preview URL and the checklist of what was verified. The merge to `master` (backend + frontend together) needs his approval of the preview, `GOOGLE_CLIENT_ID` + `APP_ADMIN_EMAILS` added to `/home/ubuntu/runners/actions-runner-betrix/betrix.env`, and the `GOOGLE_CLIENT_ID` repository variable set in GitHub. After the merge, tear down the preview: `docker rm -f betrix-preview`, remove the Nginx site + symlink, `sudo certbot delete --cert-name betrix-preview.161.118.167.148.nip.io`, `firebase hosting:channel:delete redesign`.
