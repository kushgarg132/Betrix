package com.example.gamblingsitebackend.Repository;
import com.example.gamblingsitebackend.Entity.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByUsername(String username);
}
