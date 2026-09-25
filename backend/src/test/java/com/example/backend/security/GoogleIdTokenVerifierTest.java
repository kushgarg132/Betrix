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
