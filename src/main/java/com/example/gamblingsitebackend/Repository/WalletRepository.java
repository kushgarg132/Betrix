package com.example.gamblingsitebackend.Repository;

import com.example.gamblingsitebackend.Entity.Wallet;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface WalletRepository extends MongoRepository<Wallet, String> {
    Optional<Wallet> findByUsername(String username);
}

