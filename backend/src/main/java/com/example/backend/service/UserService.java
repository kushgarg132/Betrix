package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.GoogleIdTokenVerifier.GoogleIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    static final String GOOGLE_PREFIX = "google-";

    private final UserRepository userRepository;
    private final Set<String> adminEmails;

    public UserService(UserRepository userRepository, @Value("${app.admin-emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.strip().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /** Find the account for this Google identity, or create it; refresh profile fields and role every time. */
    public User signInWithGoogle(GoogleIdentity id) {
        User user = userRepository.findByGoogleSub(id.sub()).orElseGet(() -> {
            // Legacy unique index on email (leftover from the deleted password-auth DataSeeder):
            // an old, now-unreachable password account may still hold this email. Clear it so the
            // new Google account can take the email without hitting that index.
            releaseOrphanEmail(id.email());
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
            // two first sign-ins raced; the unique googleSub index decided, use the winner's row.
            // If it's some other collision (e.g. the legacy email index against an unrelated
            // account), there's no winner to recover here - let it propagate as-is.
            return userRepository.findByGoogleSub(id.sub()).orElseThrow(() -> e);
        }
    }

    private void releaseOrphanEmail(String email) {
        if (email == null) {
            return;
        }
        // several old accounts can share one email (the old index was not always unique in practice)
        for (User orphan : userRepository.findByEmailIgnoreCaseAndGoogleSubIsNull(email)) {
            orphan.setEmail(null);
            userRepository.save(orphan);
        }
    }
}
