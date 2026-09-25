package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceRegistrationTest {

    private static final String TAKEN = "Username or email already in use";

    private UserRepository repo;
    private UserService service;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hashed");
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        service = new UserService(repo, encoder);
    }

    @Test
    void createsAUserWithHashedPasswordAndUserRole() {
        User u = service.createUser("Alice", "alice", "secret1", "alice@example.com");

        assertEquals("hashed", u.getPassword());
        assertEquals(java.util.List.of("USER"), u.getRoles());
    }

    @Test
    void reservedNamesAreRejectedInAnyCase() {
        for (String name : new String[] {"admin", "Admin", "ROOT", "bot", "Guest", "betrix"}) {
            var e = assertThrows(IllegalArgumentException.class,
                    () -> service.createUser("X", name, "secret1", "x@example.com"), name);
            assertEquals(TAKEN, e.getMessage());
        }
        verify(repo, never()).save(any());
    }

    @Test
    void usernameTakenIgnoringCaseIsRejected() {
        when(repo.existsByUsernameIgnoreCase("Alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createUser("Alice", "Alice", "secret1", "new@example.com"));
        verify(repo, never()).save(any());
    }

    @Test
    void emailTakenIgnoringCaseIsRejectedWithTheSameMessage() {
        when(repo.existsByEmailIgnoreCase("Alice@Example.com")).thenReturn(true);

        var e = assertThrows(IllegalArgumentException.class,
                () -> service.createUser("Alice", "alice2", "secret1", "Alice@Example.com"));
        assertEquals(TAKEN, e.getMessage());
    }

    /** Two requests pass the exists-checks together; the unique index rejects the second insert. */
    @Test
    void losingTheRaceIsAFriendlyErrorNotAServerError() {
        when(repo.save(any(User.class))).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        var e = assertThrows(IllegalArgumentException.class,
                () -> service.createUser("Alice", "alice", "secret1", "alice@example.com"));
        assertEquals(TAKEN, e.getMessage());
    }
}
