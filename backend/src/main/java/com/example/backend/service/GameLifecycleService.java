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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<Game> getAllGames() {
        List<Game> games = gameRepository.findAll();
        games.forEach(game -> {
            game.getPlayers().forEach(Player::hideDetails);
            game.setDeck(new com.example.backend.model.Deck());
        });
        return games;
    }

    @Transactional
    public void createGame(BlindPayload payload) {
        Game game = new Game(payload.getSmallBlindAmount(), payload.getBigBlindAmount());
        gameRepository.save(game);
        logger.debug("Game created with ID: {}", game.getId());
    }

    public Game getGameForPlayer(Game activeGame, String playerId) {
        boolean isShowdown = activeGame.getStatus() == Game.GameStatus.SHOWDOWN;

        activeGame.getPlayers().forEach(player -> {
            if ((playerId == null || !playerId.equals(player.getId())) && !isShowdown) {
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

    @Transactional
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
                    ? new Player(displayName, username, 10000)
                    : new Player(user.getName(), user.getUsername(), user.getBalance());

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

    @Transactional
    public void leaveGame(String gameId, String playerId) {
        logger.info("Player '{}' is leaving game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);

            if (game.isPlayersTurn(playerId)) {
                gameActionService.fold(game.getId(), playerId);
                // Re-load game after fold since state changed
                game = gameValidatorService.validateGameExists(gameId);
            }

            game.getPlayers().remove(player);

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.LEAVE, null, new Game(game)));

            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

            if (game.getPlayers().isEmpty()) {
                gameRepository.delete(game);
                SubscriptionResolver.cleanupGameSinks(gameId);
                logger.info("Game '{}' deleted as all players left", gameId);
                return;
            }

            gameRepository.save(game);
        } catch (Exception e) {
            logger.error("Error leaving game: {}", e.getMessage());
            throw new RuntimeException("Failed to leave game", e);
        }
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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
