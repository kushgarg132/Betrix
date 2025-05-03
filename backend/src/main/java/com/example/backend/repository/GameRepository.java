package com.example.backend.repository;

import com.example.backend.entity.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends MongoRepository<Game, String> {
    @Query(value = "{ 'status': ?0, 'players.1': { $exists: true } }", fields = "{ '_id': 1 }")
    List<Game> findIdsByStatusAndPlayersSizeGreaterThanOrEqualTwo(Game.GameStatus status);
}
