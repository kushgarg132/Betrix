package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.GoogleIdTokenVerifier.GoogleIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    /** C1(a): a leftover password-era account (no googleSub) with the same email is released first. */
    @Test
    void anOrphanAccountWithTheSameEmailIsReleasedSoTheNewGoogleUserCanTakeIt() {
        when(repo.findByGoogleSub("sub-4")).thenReturn(Optional.empty());
        User orphan = new User();
        orphan.setId("orphan-1");
        orphan.setUsername("old-password-user");
        orphan.setEmail("Shared@Example.com");
        orphan.setRoles(List.of("USER"));
        when(repo.findByEmailIgnoreCaseAndGoogleSubIsNull("shared@example.com")).thenReturn(List.of(orphan));

        User u = service.signInWithGoogle(new GoogleIdentity("sub-4", "shared@example.com", "New Person", null));

        assertNull(orphan.getEmail());
        verify(repo).save(orphan);
        assertEquals("sub-4", u.getGoogleSub());
        assertEquals("shared@example.com", u.getEmail());
    }

    /** Two leftover password-era accounts share the email: both are released, sign-in succeeds. */
    @Test
    void everyOrphanAccountWithTheSameEmailIsReleased() {
        when(repo.findByGoogleSub("sub-7")).thenReturn(Optional.empty());
        User a = new User();
        a.setId("orphan-a");
        a.setEmail("two@example.com");
        User b = new User();
        b.setId("orphan-b");
        b.setEmail("two@example.com");
        when(repo.findByEmailIgnoreCaseAndGoogleSubIsNull("two@example.com")).thenReturn(List.of(a, b));

        User u = service.signInWithGoogle(new GoogleIdentity("sub-7", "two@example.com", "Person", null));

        assertNull(a.getEmail());
        assertNull(b.getEmail());
        verify(repo).save(a);
        verify(repo).save(b);
        assertEquals("sub-7", u.getGoogleSub());
    }

    /** C1(b): an existing Google user (has a googleSub) with the same email is never touched by the release. */
    @Test
    void anExistingGoogleUserWithTheSameEmailIsNotTouchedAndTheCollisionPropagates() {
        when(repo.findByGoogleSub("sub-5")).thenReturn(Optional.empty());
        // No orphan found: the lookup only matches accounts without a googleSub, so the other
        // Google account (which has one) is excluded by construction.
        when(repo.findByEmailIgnoreCaseAndGoogleSubIsNull("dup@example.com")).thenReturn(List.of());
        DuplicateKeyException collision = new DuplicateKeyException("email index");
        when(repo.save(any(User.class))).thenThrow(collision);

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class, () ->
                service.signInWithGoogle(new GoogleIdentity("sub-5", "dup@example.com", "Person", null)));

        assertEquals(collision, thrown);
    }

    /** C1(c): the pre-existing googleSub race (two concurrent first sign-ins) still resolves to the winner. */
    @Test
    void concurrentFirstSignInsStillResolveToTheRaceWinner() {
        User winner = new User();
        winner.setId("winner-1");
        winner.setGoogleSub("sub-6");
        winner.setUsername("google-sub-6");
        winner.setRoles(List.of("USER"));
        when(repo.findByGoogleSub("sub-6")).thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
        when(repo.save(any(User.class))).thenThrow(new DuplicateKeyException("googleSub index"));

        User u = service.signInWithGoogle(new GoogleIdentity("sub-6", "race@example.com", "Racer", null));

        assertEquals("winner-1", u.getId());
    }
}
