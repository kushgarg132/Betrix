package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.GameService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
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
    
    private final GameService gameService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> createGame() {
        Game game = gameService.createGame();
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> joinGame(Authentication authentication,
                                         @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        String username = authentication.getName();
        Game userGame = gameService.joinGame(gameId, username);
        return ResponseEntity.ok(userGame);
    }

    @PostMapping("/{gameId}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> startNewHand(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        gameService.startNewHand(gameId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Game>> getAllGames() {
        List<Game> game = gameService.getAllGames();
        return ResponseEntity.ok(game);
    }
    
    @GetMapping("/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> getGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        Game game = gameService.getGameForPlayer(gameId , null);
        return ResponseEntity.ok(game);
    }
    
    @GetMapping("/{gameId}/player/{playerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> getGameForPlayer(
            @PathVariable String gameId,
            @PathVariable String playerId) {
        Game game = gameService.getGameForPlayer(gameId, playerId);
        return ResponseEntity.ok(game);
    }

    @DeleteMapping("/{gameId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        gameService.deleteGame(gameId);
        return ResponseEntity.ok().build();
    }
}