package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.service.GameService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/getAll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Game>> getAllGames() {
        List<Game> game = gameService.getAllGames();
        return ResponseEntity.ok(game);
    }
    
    @GetMapping("/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> getGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        Game game = gameService.getGame(gameId);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping("/{gameId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> joinGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId,
            @RequestParam @NotBlank(message = "Player ID is required") String playerId) {
        Game game = gameService.joinGame(gameId, playerId);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping("/{gameId}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> startNewHand(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId) {
        Game game = gameService.startNewHand(gameId);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping("/{gameId}/bet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> placeBet(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId,
            @RequestParam @NotBlank(message = "Player ID is required") String playerId,
            @RequestParam @Min(value = 0, message = "Bet amount must be positive") double amount) {
        Game game = gameService.placeBet(gameId, playerId, amount);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping("/{gameId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> check(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId,
            @RequestParam @NotBlank(message = "Player ID is required") String playerId) {
        Game game = gameService.placeBet(gameId, playerId, 0);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping("/{gameId}/fold")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> fold(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId,
            @RequestParam @NotBlank(message = "Player ID is required") String playerId) {
        Game game = gameService.fold(gameId, playerId);
        return ResponseEntity.ok(game);
    }
    
    @PostMapping("/{gameId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Game> leaveGame(
            @PathVariable @NotBlank(message = "Game ID is required") String gameId,
            @RequestParam @NotBlank(message = "Player ID is required") String playerId) {
        Game game = gameService.leaveGame(gameId, playerId);
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