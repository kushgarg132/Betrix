package com.example.gamblingsitebackend.Repository;

import com.example.gamblingsitebackend.Entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    @Override
    Optional<User> findById(String s);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);
}
