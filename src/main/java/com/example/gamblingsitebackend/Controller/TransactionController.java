package com.example.gamblingsitebackend.Controller;

import com.example.gamblingsitebackend.Entity.Transaction;
import com.example.gamblingsitebackend.Service.TransactionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/history")
    public List<Transaction> getTransactionHistory(@RequestParam String username) {
        return transactionService.getTransactionHistory(username);
    }
}

