package com.example.gamblingsitebackend.Controller;

import com.example.gamblingsitebackend.Service.WalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/create")
    public Double getWalletBalance(@RequestParam String username , @RequestParam double initialBalance) {
        return walletService.createWallet(username, initialBalance).getBalance();
    }

    @GetMapping("/balance")
    public Double getWalletBalance(@RequestParam String username) {
        return walletService.getBalance(username);
    }

    @PostMapping("/deposit")
    public double deposit(@RequestParam String username, @RequestParam double amount) {
        return walletService.deposit(username, amount);
    }

    @PostMapping("/withdraw")
    public double withdraw(@RequestParam String username, @RequestParam double amount) {
        return walletService.withdraw(username, amount);
    }
}

