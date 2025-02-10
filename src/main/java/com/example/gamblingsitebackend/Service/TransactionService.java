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

    public void recordTransaction(String walletId, String username, String type, double amount, String description) {
        Transaction transaction = new Transaction(
                null,
                walletId,
                username,
                type,
                amount,
                description,
                LocalDateTime.now()
        );
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(String username) {
        return transactionRepository.findByUsername(username);
    }
}
