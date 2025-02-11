package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.Wallet;
import com.example.gamblingsitebackend.Repository.UserRepository;
import com.example.gamblingsitebackend.Repository.WalletRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    public WalletService(WalletRepository walletRepository, TransactionService transactionService, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
    }

    public Wallet createWallet(String username, double initialBalance) {
        Optional<Wallet> wallet = walletRepository.findByUsername(username);
        if (wallet.isPresent()) {
            throw new RuntimeException("Wallet already exists.");
        }
        return walletRepository.save(new Wallet(null, username, initialBalance));
    }

    public double getBalance(String username) {
        return walletRepository.findByUsername(username)
                .map(Wallet::getBalance)
                .orElse(0.0);
    }

    public double deposit(String username, double amount) {
        Optional<Wallet> walletOptional = walletRepository.findByUsername(username);
        if (walletOptional.isEmpty()) {
            throw new IllegalArgumentException("Wallet not found");
        }
        Wallet wallet = walletOptional.get();
        wallet.setBalance(wallet.getBalance()+amount);
        walletRepository.save(wallet);

        transactionService.recordTransaction(username, "DEPOSIT", amount, "Deposit to wallet");
        return wallet.getBalance();
    }

    public double withdraw(String username, double amount) {
        Optional<Wallet> walletOptional = walletRepository.findByUsername(username);
        if (walletOptional.isEmpty()) {
            throw new IllegalArgumentException("Wallet not found");
        }
        Wallet wallet = walletOptional.get();
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        transactionService.recordTransaction(username, "WITHDRAW", amount, "Withdrawal from wallet");
        return wallet.getBalance();
    }
}

