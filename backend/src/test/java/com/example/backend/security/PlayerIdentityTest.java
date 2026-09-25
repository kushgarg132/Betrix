package com.example.backend.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerIdentityTest {

    @Test
    void classifiesUsernamesByPrefix() {
        assertTrue(PlayerIdentity.isGuest("guest-abc123"));
        assertFalse(PlayerIdentity.isGuest("guestperson"));
        assertFalse(PlayerIdentity.isGuest(null));

        assertTrue(PlayerIdentity.isBot("bot-abc123"));
        assertFalse(PlayerIdentity.isBot("robot-x"));
        assertFalse(PlayerIdentity.isBot(null));

        assertFalse(PlayerIdentity.isGuest("bot-abc123"));
        assertFalse(PlayerIdentity.isBot("guest-abc123"));
    }

    @Test
    void generatedUsernamesCarryTheirOwnPrefixAndNothingElse() {
        String guest = PlayerIdentity.newGuestUsername();
        String bot = PlayerIdentity.newBotUsername();

        assertTrue(PlayerIdentity.isGuest(guest));
        assertFalse(PlayerIdentity.isBot(guest));
        assertTrue(Pattern.matches("guest-[0-9a-f]{12}", guest), guest);

        assertTrue(PlayerIdentity.isBot(bot));
        assertFalse(PlayerIdentity.isGuest(bot));
        assertTrue(Pattern.matches("bot-[0-9a-f]{6}", bot), bot);

        assertFalse(guest.equals(PlayerIdentity.newGuestUsername()));
    }

    @Test
    void guestAuthoritiesMatchWhatHasRoleGuestWouldExpect() {
        // Spring's hasRole('GUEST') checks for the authority "ROLE_GUEST", never bare "GUEST".
        // JwtAuthenticationFilter used "GUEST" and GraphQLAuthInterceptor used "ROLE_GUEST" -- only
        // one of the two would ever have satisfied hasRole('GUEST').
        List<String> authorities = PlayerIdentity.guestAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority).toList();

        assertEquals(List.of("ROLE_GUEST"), authorities);
    }
}
