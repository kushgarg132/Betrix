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
    @Query(value = "{ 'status': ?0, 'players.1': { $exists: true } }", fields = "{ '_id': 1 }")
    List<Game> findIdsByStatusAndPlayersSizeGreaterThanOrEqualTwo(Game.GameStatus status);
    
    /**
     * Find all games where the current player's action timeout has expired
     */
    @Query("{ 'status': { $in: ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'] }, " +
           "'currentPlayerActionDeadline': { $lt: ?0 } }")
    List<Game> findGamesWithPlayerActionTimeout(Instant now);
    
    /**
     * Find all games where the current player's action timeout will expire soon
     */
    @Query("{ 'status': { $in: ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'] }, " +
           "'currentPlayerActionDeadline': { $gt: ?0, $lt: ?1 } }")
    List<Game> findGamesWithImminentPlayerActionTimeout(Instant from, Instant to);
    
    /**
     * Find all games that have been idle for too long
     */
    @Query("{ 'lastActivityTime': { $lt: ?0 } }")
    List<Game> findIdleGames(LocalDateTime idleThreshold);
    
    /**
     * Find all games that are ready for auto-start (waiting status, enough players, auto-start enabled)
     */
    @Query("{ 'status': 'WAITING', 'players.1': { $exists: true }, 'autoStart': true }")
    List<Game> findGamesReadyForAutoStart();
    
    /**
     * Find active games that are in progress
     */
    @Query("{ 'status': { $in: ['STARTING', 'PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'] } }")
    List<Game> findActiveGames();
    
    /**
     * Count games by status (for scheduler metrics)
     */
    @Query(value = "{ 'status': ?0 }", count = true)
    long countGamesByStatus(Game.GameStatus status);
}
