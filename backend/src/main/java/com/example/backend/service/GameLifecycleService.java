package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.event.PlayerActionEvent;
import com.example.backend.event.PlayerJoinedEvent;
import com.example.backend.model.BlindPayload;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.resolver.SubscriptionResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameLifecycleService {
    private static final Logger logger = LoggerFactory.getLogger(GameLifecycleService.class);

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GameValidatorService gameValidatorService;
    private final GameEventPublisher eventPublisher;
    private final GameActionService gameActionService;

    private static final int MAX_BLIND = 1_000_000;

    /** Play money: every player, guest or registered, sits down with the same stack. */
    @Value("${game.buy-in:10000}")
    private long buyIn;

    public List<Game> getAllGames() {
        List<Game> games = gameRepository.findAll();
        games.forEach(game -> {
            game.getPlayers().forEach(Player::hideDetails);
            game.setDeck(new com.example.backend.model.Deck());
        });
        return games;
    }

    public void createGame(BlindPayload payload) {
        int small = payload.getSmallBlindAmount();
        int big = payload.getBigBlindAmount();
        if (small < 1 || big < small || big > MAX_BLIND) {
            throw new IllegalArgumentException("Blinds must satisfy 1 <= small blind <= big blind <= " + MAX_BLIND);
        }
        Game game = new Game(payload.getSmallBlindAmount(), payload.getBigBlindAmount());
        gameRepository.save(game);
        logger.debug("Game created with ID: {}", game.getId());
    }

    public Game getGameForPlayer(Game activeGame, String playerId) {
        boolean reveal = activeGame.showdownRevealsHands();

        activeGame.getPlayers().forEach(player -> {
            boolean isViewer = playerId != null && playerId.equals(player.getId());
            if (!isViewer && (!reveal || player.isHasFolded())) {
                player.hideDetails();
            }
        });
        activeGame.setDeck(null);
        return activeGame;
    }

    public Game getGameForPlayer(String gameId, String playerId) {
        Game game = gameValidatorService.validateGameExists(gameId);
        return getGameForPlayer(game, playerId);
    }

    public Game joinGame(String gameId, String username) {
        try {
            boolean isGuest = username != null && (username.startsWith("guest-") || username.startsWith("bot-"));
            com.example.backend.entity.User user = null;
            if (!isGuest) {
                user = userRepository.findByUsername(username).orElseThrow(() ->
                        new RuntimeException("User not found: " + username));
            }

            Game game = gameValidatorService.validateGameExists(gameId);
            gameValidatorService.validateGameNotFull(game);

            if (game.hasPlayer(username)) {
                return getGameForPlayer(game, game.getPlayerByUsername(username).getId());
            }

            String displayName = username.startsWith("bot-") ? "Bot" : "Guest";
            Player player = isGuest
                    ? new Player(displayName, username, buyIn)
                    : new Player(user.getName(), user.getUsername(), buyIn);

            if (game.getStatus() != Game.GameStatus.WAITING) {
                player.setActive(false);
            }

            game.getPlayers().add(player);
            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            eventPublisher.publishEvent(new PlayerJoinedEvent(gameId, player));
            return getGameForPlayer(game, player.getId());
        } catch (Exception e) {
            logger.error("Error joining game: {}", e.getMessage());
            throw new RuntimeException("Failed to join game", e);
        }
    }

    public void leaveGame(String gameId, String playerId) {
        logger.info("Player '{}' is leaving game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);

            if (handInProgress(game)) {
                // What this player has put in this round is still part of the pot calculation, so they
                // stay seated (folded) until the hand ends; BettingManager removes leaving players then.
                player.setLeaving(true);
                gameRepository.save(game);
                if (!player.isHasFolded() && player.isActive()) {
                    if (game.isPlayersTurn(playerId)) {
                        gameActionService.fold(gameId, playerId);
                    } else {
                        gameActionService.foldOutOfTurn(gameId, playerId);
                    }
                }
                // Re-load: the fold may have ended the hand, which removes leaving players
                game = gameValidatorService.validateGameExists(gameId);
            } else {
                game.removePlayer(playerId);
            }

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.LEAVE, null, new Game(game)));

            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

            if (game.getPlayers().isEmpty()) {
                gameRepository.delete(game);
                SubscriptionResolver.cleanupGameSinks(gameId);
                logger.info("Game '{}' deleted as all players left", gameId);
                return;
            }

            if (!handInProgress(game)) {
                gameRepository.save(game);
            }
        } catch (Exception e) {
            logger.error("Error leaving game: {}", e.getMessage());
            throw new RuntimeException("Failed to leave game", e);
        }
    }

    private static boolean handInProgress(Game game) {
        return game.getStatus() != Game.GameStatus.WAITING && game.getStatus() != Game.GameStatus.FINISHED;
    }

    public void sitOut(String gameId, String playerId) {
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);
            player.setSittingOut(true);
            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);
            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.SIT_OUT, null, new Game(game)));
        } catch (Exception e) {
            logger.error("Error sitting out: {}", e.getMessage());
            throw new RuntimeException("Failed to sit out", e);
        }
    }

    public void sitIn(String gameId, String playerId) {
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);
            player.setSittingOut(false);
            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);
            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.SIT_IN, null, new Game(game)));
        } catch (Exception e) {
            logger.error("Error sitting in: {}", e.getMessage());
            throw new RuntimeException("Failed to sit in", e);
        }
    }

    public boolean deleteGame(String gameId) {
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            gameRepository.delete(game);
            SubscriptionResolver.cleanupGameSinks(gameId);
            logger.debug("Game '{}' deleted", gameId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting game: {}", e.getMessage());
            throw new RuntimeException("Failed to delete game", e);
        }
    }
}
