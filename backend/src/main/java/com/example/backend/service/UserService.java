package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    // Names that read as system or staff accounts. The message below is deliberately the same as
    // for a taken name so this list can't be probed.
    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "root", "system", "support", "betrix", "guest", "bot");

    private static final String TAKEN = "Username or email already in use";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
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
    public User addBalance(String username, int amount) {
        User user = userRepository.findByUsername(username).get();
        user.setBalance(user.getBalance() + amount);
        return userRepository.save(user);
    }
}