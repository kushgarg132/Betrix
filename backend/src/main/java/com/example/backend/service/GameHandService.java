package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.event.CardsDealtEvent;
import com.example.backend.event.GameStartedEvent;
import com.example.backend.model.Card;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameHandService {
    private static final Logger logger = LoggerFactory.getLogger(GameHandService.class);

    private final GameRepository gameRepository;
    private final GameValidatorService gameValidatorService;
    private final BettingManager bettingManager;
    private final GameEventPublisher eventPublisher;
    private final GameLifecycleService gameLifecycleService;

    @Transactional
    public void startNewHand(String gameId) {
        logger.info("Starting a new hand for game '{}'", gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);

            // Auto-kick players with insufficient chips
            List<Player> toKick = new ArrayList<>();
            for (Player player : game.getPlayers()) {
                if (player.getChips() < game.getBigBlindAmount()) {
                    toKick.add(player);
                }
            }
            for (Player player : toKick) {
                logger.info("Auto-kicking player {} due to insufficient funds", player.getUsername());
                gameLifecycleService.leaveGame(gameId, player.getId());
            }

            long activePlayersCount = game.getPlayers().stream().filter(p -> !p.isSittingOut()).count();
            if (activePlayersCount < 2) {
                logger.warn("Not enough active players. Setting game back to WAITING.");
                game.setStatus(Game.GameStatus.WAITING);
                gameRepository.save(game);
                return;
            }

            if (game.getStatus() != Game.GameStatus.WAITING) {
                throw new RuntimeException("Game already in progress");
            }

            game.resetForNewHand();
            game.setStatus(Game.GameStatus.STARTING);

            for (Player player : game.getPlayers()) {
                player.reset();
                if (!player.isSittingOut() && player.getChips() > 0) {
                    player.setActive(true);
                    player.addCard(game.getDeck().drawCard());
                    player.addCard(game.getDeck().drawCard());
                    game.getMainPot().addEligiblePlayer(player.getId());
                } else {
                    player.setActive(false);
                }
            }

            eventPublisher.publishEvent(new GameStartedEvent(gameId, new Game(game)));

            Map<String, List<Card>> playerCards = new HashMap<>();
            game.getPlayers().forEach(player -> {
                if (player.isActive() && player.getHand() != null && !player.getHand().isEmpty()) {
                    playerCards.put(player.getId(), new ArrayList<>(player.getHand()));
                }
            });
            eventPublisher.publishEvent(new CardsDealtEvent(gameId, playerCards));

            bettingManager.startNewBettingRound(game);
            gameRepository.save(game);
        } catch (Exception e) {
            logger.error("Error starting new hand: {}", e.getMessage());
            throw new RuntimeException("Failed to start new hand", e);
        }
    }

    @Transactional
    public void executeAllInAction(String gameId) {
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            bettingManager.processAllInRound(game);
            gameRepository.save(game);
        } catch (Exception e) {
            logger.error("Error executing all-in action: {}", e.getMessage());
        }
    }
}
