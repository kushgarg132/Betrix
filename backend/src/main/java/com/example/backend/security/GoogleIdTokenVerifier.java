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
