package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.GoogleIdTokenVerifier.GoogleIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    // Names that read as system or staff accounts. The message below is deliberately the same as
    // for a taken name so this list can't be probed.
    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "root", "system", "support", "betrix", "guest", "bot");

    private static final String TAKEN = "Username or email already in use";

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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
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

    public User createUser(String name,String username, String password, String email) {
        if (RESERVED.contains(username.toLowerCase(Locale.ROOT))
                || userRepository.existsByUsernameIgnoreCase(username)
                || userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(TAKEN);
        }

        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setBalance(1000);
        user.setRoles(Collections.singletonList("USER"));

        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // two registrations raced past the check above; the unique index decided
            throw new IllegalArgumentException(TAKEN);
        }
    }
}
