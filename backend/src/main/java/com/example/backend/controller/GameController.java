package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.model.BlindPayload;
import com.example.backend.service.GameService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@Validated
@RequiredArgsConstructor
public class GameController {
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    private final GameService gameService;

    @PostMapping("/create-new")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createGame(@RequestBody BlindPayload payload) {
        logger.info("Request received to create a new game");
        try {
            gameService.createGame(payload);
            logger.debug("New game created");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error creating game: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{gameId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> joinGame(Authentication authentication,
                                         @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        String username = authentication.getName();
        logger.info("User '{}' is attempting to join game with ID: {}", username, gameId);
        try {
            Game game = gameService.joinGame(gameId, username);
            logger.debug("User '{}' joined game: {}", username, game);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            logger.error("Error joining game: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Game>> getAllGames() {
        logger.info("Request received to fetch all games");
        try {
            List<Game> games = gameService.getAllGames();
            logger.debug("Fetched games: {}", games);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            logger.error("Error fetching games: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> getGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        logger.info("Request received to fetch game with ID: {}", gameId);
        try {
            Game game = gameService.getGameForPlayer(gameId, null);
            logger.debug("Fetched game: {}", game);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            logger.error("Error fetching game: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{gameId}/player/{playerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> getGameForPlayer(
            @PathVariable String gameId,
            @PathVariable String playerId) {
        try {
            Game game = gameService.getGameForPlayer(gameId, playerId);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            logger.error("Error fetching game for player: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{gameId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        logger.info("Request received to delete game with ID: {}", gameId);
        try {
            gameService.deleteGame(gameId);
            logger.debug("Game with ID '{}' deleted", gameId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting game: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}