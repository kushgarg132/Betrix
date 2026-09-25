package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederTest {

    private UserRepository users;
    private IndexOperations indexes;
    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        indexes = mock(IndexOperations.class);
        when(mongo.indexOps(User.class)).thenReturn(indexes);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("hashed");
        seeder = new DataSeeder(users, encoder, mongo);
        ReflectionTestUtils.setField(seeder, "adminUsername", "admin");
        ReflectionTestUtils.setField(seeder, "adminEmail", "admin@example.com");
    }

    @Test
    void refusesToCreateAnAdminWithAMissingOrShortPassword() {
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        for (String weak : new String[] {"", "admin123", "change-me", "elevenchars"}) {
            ReflectionTestUtils.setField(seeder, "adminPassword", weak);
            assertThrows(IllegalStateException.class, () -> seeder.run(null), weak);
        }
        verify(users, never()).save(any());
    }

    @Test
    void createsTheAdminWhenThePasswordIsStrong() {
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(seeder, "adminPassword", "a-long-random-secret");

        seeder.run(null);

        verify(users).save(any(User.class));
    }

    @Test
    void anExistingAdminDoesNotNeedThePasswordConfigured() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(new User()));
        ReflectionTestUtils.setField(seeder, "adminPassword", "");

        assertDoesNotThrow(() -> seeder.run(null));
        verify(users, never()).save(any());
    }

    @Test
    void createsUniqueIndexesOnUsernameAndEmail() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(new User()));

        seeder.run(null);

        verify(indexes, times(2)).ensureIndex(any(Index.class));
    }

    @Test
    void indexFailureFromExistingDuplicateDataDoesNotStopStartup() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(indexes.ensureIndex(any(Index.class))).thenThrow(new RuntimeException("E11000 duplicate key"));

        assertDoesNotThrow(() -> seeder.run(null));
    }
}
