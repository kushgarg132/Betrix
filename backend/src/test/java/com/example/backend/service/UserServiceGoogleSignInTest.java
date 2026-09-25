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
