package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.Wallet;
import com.example.gamblingsitebackend.Repository.WalletRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    public WalletService(WalletRepository walletRepository, TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
    }

    public void createWallet(String username, double initialBalance) {
        Optional<Wallet> wallet = walletRepository.findByUsername(username);
        if (wallet.isEmpty()) {
            walletRepository.save(new Wallet(null, username, initialBalance));
        }
    }

    public double getBalance(String username) {
        return walletRepository.findByUsername(username)
                .map(Wallet::getWalletBalance)
                .orElse(0.0);
    }

    public double deposit(String username, double amount) {
        Wallet wallet = walletRepository.findByUsername(username)
                .orElse(new Wallet(null, username, 0.0));
        wallet.setWalletBalance(wallet.getWalletBalance() + amount);
        walletRepository.save(wallet);

        transactionService.recordTransaction(wallet.getId(), username, "DEPOSIT", amount, "Deposit to wallet");
        return wallet.getWalletBalance();
    }

    public double withdraw(String username, double amount) {
        Wallet wallet = walletRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        if (wallet.getWalletBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        wallet.setWalletBalance(wallet.getWalletBalance() - amount);
        walletRepository.save(wallet);

        transactionService.recordTransaction(wallet.getId(), username, "WITHDRAW", amount, "Withdrawal from wallet");
        return wallet.getWalletBalance();
    }
}

