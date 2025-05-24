package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(GameValidatorService.class);

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public Game validateGameExists(String gameId) {
        if (gameId == null || gameId.trim().isEmpty()) {
            logger.error("Game ID cannot be null or empty");
            throw new IllegalArgumentException("Game ID cannot be null or empty");
        }
        return gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    logger.error("Game not found with ID: {}", gameId);
                    return new RuntimeException("Game not found: " + gameId);
                });
    }

    public void validateGameNotFull(Game game) {
        if (game.isGameFull()) {
            logger.error("Game is full");
            throw new RuntimeException("Game is full");
        }
    }

    public void validateGameStatus(Game game, Game.GameStatus expectedStatus) {
        if (game.getStatus() != expectedStatus) {
            logger.error("Invalid game status: expected {}, but was {}", expectedStatus, game.getStatus());
            throw new RuntimeException("Invalid game status");
        }
    }

    public Player validatePlayerExists(Game game, String playerId) {
        if (playerId == null || playerId.isEmpty()) {
            logger.error("Player ID cannot be null or empty");
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("Player not found in game: {}", playerId);
                    return new RuntimeException("Player not found in game: " + playerId);
                });
    }

    public void validatePlayerTurn(Game game, String playerId) {
        if (!game.isPlayersTurn(playerId)) {
            logger.error("Not player's turn");
            throw new RuntimeException("Not player's turn");
        }
    }

    public void validatePlayerNotFolded(Player player) {
        if (player.isHasFolded()) {
            logger.error("Player has folded");
            throw new RuntimeException("Player has folded");
        }
    }

    public void validatePlayerBetAmount(Game game, Player player, double amount) {
        double betPlacedAmount = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername(), 0.0);
        double betPlacedTotal = betPlacedAmount + amount;
        if (amount < 0) {
            logger.error("Bet amount cannot be negative");
            throw new IllegalArgumentException("Bet amount cannot be negative");
        }else if(amount > player.getChips()){
            logger.error("Bet amount cannot be greater than Balance");
            throw new IllegalArgumentException("Bet amount cannot be greater than Balance");
        }else if(betPlacedTotal < game.getCurrentBet() && player.getChips() > amount) {
            logger.error("Bet amount cannot be greater than Balance");
            throw new IllegalArgumentException("Bet amount cannot be greater than Balance");
        }
    }
}