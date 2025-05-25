package com.example.backend.repository;

import com.example.backend.entity.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameRepository extends MongoRepository<Game, String> {
    /**
     * Find all games that are ready for auto-start (waiting status, enough players, auto-start enabled)
     */
    @Query("{ 'status': 'WAITING', 'players.1': { $exists: true }, 'autoStart': true }")
    List<Game> findGamesReadyForAutoStart();
    
    /**
     * Find active games that are in progress
     */
    @Query("{ 'status': { $in: ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'] } }")
    List<Game> findActiveGames();
}
