package com.example.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // HS512 needs a key of at least 512 bits. Tokens are signed with the base64-decoded secret.
    private static final int MIN_SECRET_BYTES = 64;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey key;

    /** Fail at startup, not on the first login: a weak or malformed secret used to boot fine and then break every login. */
    @PostConstruct
    void init() {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(jwtSecret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("app.jwt.secret must be base64, e.g. from: openssl rand -base64 64");
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must decode to at least " + MIN_SECRET_BYTES
                    + " bytes (HS512). Generate one with: openssl rand -base64 64");
        }
        key = Keys.hmacShaKeyFor(bytes); // 64+ bytes selects HS512, same as before
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Generate a token for a guest user with minimal claims.
     */
    public String generateGuestToken(String username) {
        Date now = new Date();

        return Jwts.builder()
                .subject(username)
                .claim("guest", true)
                .claim("roles", Collections.singletonList("ROLE_GUEST"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
