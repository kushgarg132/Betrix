package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.Transaction;
import com.example.gamblingsitebackend.Repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void recordTransaction(String username, String type, double amount, String description) {
        Transaction transaction = new Transaction(
                null,
                username,
                amount,
                LocalDateTime.now(),
                type,
                description
                );
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(String username) {
        return transactionRepository.findByUsername(username);
    }
}
