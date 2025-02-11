package com.example.gamblingsitebackend.GameController;

import com.example.gamblingsitebackend.Model.GameResult;
import com.example.gamblingsitebackend.Service.WalletService;
import org.springframework.web.bind.annotation.*;
import java.util.Random;

@RestController
@RequestMapping("/api/games/dice")
public class GameController {

    private final WalletService walletService;
    private final Random random = new Random();

    public GameController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/play")
    public GameResult playDice(@RequestParam String username, @RequestParam int betNumber, @RequestParam double betAmount) {
        if (betAmount > walletService.getBalance(username)) {
            return new GameResult("Insufficient balance", walletService.getBalance(username));
        }
        if (betNumber < 1 || betNumber > 6) {
            return new GameResult("Invalid bet number. Choose between 1 and 6.", walletService.getBalance(username));
        }

        int rolledNumber = random.nextInt(6) + 1;

        if (rolledNumber == betNumber) {
            double winnings = betAmount * 5;
            walletService.deposit(username, winnings);
            return new GameResult("Congrats! You won! The dice rolled: " + rolledNumber, walletService.getBalance(username));
        } else {
            walletService.withdraw(username, betAmount);
            return new GameResult("You lost. The dice rolled: " + rolledNumber, walletService.getBalance(username));
        }
    }
}


