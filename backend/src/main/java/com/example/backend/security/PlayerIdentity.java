package com.example.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Guest and bot players are never rows in the users collection — they exist only as a username
 * prefix recognised on sight. One definition of that prefix, used everywhere a caller needs to
 * tell a guest or bot apart from a registered user, instead of five separate startsWith checks
 * that could quietly drift apart.
 */
public final class PlayerIdentity {

    public static final String GUEST_PREFIX = "guest-";
    public static final String BOT_PREFIX = "bot-";

    private PlayerIdentity() {
    }

    public static boolean isGuest(String username) {
        return username != null && username.startsWith(GUEST_PREFIX);
    }

    public static boolean isBot(String username) {
        return username != null && username.startsWith(BOT_PREFIX);
    }

    /** True for either — the two cases that get 10000 free chips and no persisted User row. */
    public static boolean isTransient(String username) {
        return isGuest(username) || isBot(username);
    }

    public static String newGuestUsername() {
        return GUEST_PREFIX + shortId(12);
    }

    public static String newBotUsername() {
        return BOT_PREFIX + shortId(6);
    }

    private static String shortId(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    /** The one authority a guest token carries. Matches what @PreAuthorize("hasRole('GUEST')") expects. */
    public static List<GrantedAuthority> guestAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));
    }
}
