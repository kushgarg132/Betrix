package com.example.gamblingsitebackend.GameController;

import com.example.gamblingsitebackend.Model.GameResult;
import com.example.gamblingsitebackend.Service.WalletService;
import org.springframework.web.bind.annotation.*;
import java.util.Random;

@RestController
@RequestMapping("/api/roulette")
public class RouletteController {

    private final WalletService walletService;
    private final Random random = new Random();

    public RouletteController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/play")
    public GameResult playRoulette(@RequestParam String username, @RequestParam int betNumber, @RequestParam double betAmount) {
        if (betAmount > walletService.getBalance(username)) {
            return new GameResult("Insufficient balance", walletService.getBalance(username));
        }
        if (betNumber < 0 || betNumber > 36) {
            return new GameResult("Invalid bet. Choose a number between 0 and 36.", walletService.getBalance(username));
        }

        int rolledNumber = random.nextInt(37);

        if (rolledNumber == betNumber) {
            double winnings = betAmount * 35;
            walletService.deposit(username, winnings);
            return new GameResult("Congrats! You won! The ball landed on: " + rolledNumber, walletService.getBalance(username));
        } else {
            walletService.withdraw(username, betAmount);
            return new GameResult("You lost. The ball landed on: " + rolledNumber, walletService.getBalance(username));
        }
    }
}

