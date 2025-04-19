
package com.example.backend.controller;

import com.example.backend.model.Game;
import com.example.backend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    @PostMapping("/create")
    public ResponseEntity<Game> createGame() {
        return ResponseEntity.ok(gameService.createGame());
    }
    
    @PostMapping("/join")
    public ResponseEntity<Game> joinGame(@RequestParam String gameId, @RequestParam String playerId) {
        return ResponseEntity.ok(gameService.joinGame(gameId, playerId));
    }
    
    @PostMapping("/bet")
    public ResponseEntity<Game> placeBet(
            @RequestParam String gameId,
            @RequestParam String playerId,
            @RequestParam double amount) {
        return ResponseEntity.ok(gameService.placeBet(gameId, playerId, amount));
    }
    
    @PostMapping("/fold")
    public ResponseEntity<Game> fold(@RequestParam String gameId, @RequestParam String playerId) {
        return ResponseEntity.ok(gameService.fold(gameId, playerId));
    }
    
    @PostMapping("/deal")
    public ResponseEntity<Game> dealCards(@RequestParam String gameId) {
        return ResponseEntity.ok(gameService.dealCards(gameId));
    }
    
    @PostMapping("/next-round")
    public ResponseEntity<Game> nextRound(@RequestParam String gameId) {
        return ResponseEntity.ok(gameService.nextRound(gameId));
    }
}