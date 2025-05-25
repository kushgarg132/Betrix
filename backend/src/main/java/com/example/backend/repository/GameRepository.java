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
     * Alternative method to find games ready for auto-start
     * This uses a simpler query that might work better with MongoDB
     */
    @Query("{ 'status': 'WAITING', 'autoStart': true }")
    List<Game> findWaitingGamesWithAutoStart();
}
