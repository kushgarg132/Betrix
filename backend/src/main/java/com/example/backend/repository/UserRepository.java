package com.example.backend.repository;

import com.example.backend.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    Optional<User> findByGoogleSub(String googleSub);

    /** Old password-era accounts (no googleSub) that still hold this email, for the legacy unique index. */
    Optional<User> findByEmailIgnoreCaseAndGoogleSubIsNull(String email);
}
