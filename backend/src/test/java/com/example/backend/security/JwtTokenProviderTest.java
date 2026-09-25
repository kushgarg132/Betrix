package com.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static String secretOfBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private static JwtTokenProvider provider(String secret, int expirationMs) {
        JwtTokenProvider p = new JwtTokenProvider();
        ReflectionTestUtils.setField(p, "jwtSecret", secret);
        ReflectionTestUtils.setField(p, "jwtExpirationMs", expirationMs);
        p.init();
        return p;
    }

    private static String tokenFor(JwtTokenProvider p, String username) {
        var principal = new User(username, "x", List.of());
        return p.generateToken(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void roundTripsUsernameForRegisteredAndGuestTokens() {
        JwtTokenProvider p = provider(secretOfBytes(64), 60_000);

        String t = tokenFor(p, "alice");
        assertTrue(p.validateToken(t));
        assertEquals("alice", p.getUsernameFromToken(t));

        String g = p.generateGuestToken("guest-abc123");
        assertTrue(p.validateToken(g));
        assertEquals("guest-abc123", p.getUsernameFromToken(g));
    }

    /**
     * .env.example used to say `openssl rand -base64 48` (384 bits). The library refuses that for HS512, so
     * the app used to start fine and then fail every login. It must now refuse to start, with a clear message.
     */
    @Test
    void aSecretTooShortForHs512StopsStartupInsteadOfBreakingEveryLogin() {
        var e = assertThrows(IllegalStateException.class, () -> provider(secretOfBytes(48), 60_000));
        assertTrue(e.getMessage().contains("base64 64"), e.getMessage());
    }

    @Test
    void aSecretThatIsNotBase64StopsStartup() {
        assertThrows(IllegalStateException.class, () -> provider("change-me-openssl-rand-base64-48!!", 60_000));
        assertThrows(IllegalStateException.class, () -> provider("", 60_000));
    }

    @Test
    void aSixtyFourByteSecretIsTheMinimumAndWorks() {
        JwtTokenProvider p = provider(secretOfBytes(64), 60_000);

        assertTrue(p.validateToken(tokenFor(p, "alice")));
    }

    /** Tokens issued before this change were HS512 over these same key bytes; they must stay valid across the deploy. */
    @Test
    void tokensIssuedByTheOldImplementationStillValidate() {
        String secret = secretOfBytes(96);
        JwtTokenProvider p = provider(secret, 60_000);
        @SuppressWarnings("deprecation")
        String oldToken = io.jsonwebtoken.Jwts.builder().setSubject("alice")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, secret).compact();

        assertTrue(p.validateToken(oldToken));
        assertEquals("alice", p.getUsernameFromToken(oldToken));
    }

    @Test
    void rejectsTamperedExpiredForeignAndUnsignedTokens() {
        JwtTokenProvider p = provider(secretOfBytes(64), 60_000);
        String good = tokenFor(p, "alice");

        String[] parts = good.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
        assertFalse(p.validateToken(parts[0] + "." + forgedPayload + "." + parts[2]), "tampered payload");

        assertFalse(p.validateToken(tokenFor(provider(secretOfBytes(64), 60_000), "alice")), "other secret");

        assertFalse(p.validateToken(tokenFor(provider(((String) ReflectionTestUtils.getField(p, "jwtSecret")), -1000), "alice")),
                "expired");

        String none = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
                + "." + forgedPayload + ".";
        assertFalse(p.validateToken(none), "alg none");

        assertFalse(p.validateToken("not-a-jwt"));
        assertFalse(p.validateToken(""));
    }
}
