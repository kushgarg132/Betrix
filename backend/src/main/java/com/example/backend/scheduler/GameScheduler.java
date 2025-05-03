package com.example.backend.scheduler;

import com.example.backend.entity.Game;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameScheduler {
    private final GameRepository gameRepository;
    private final GameService gameService;

    @Scheduled(fixedRate = 10000)
    public void startWaitingGames() {
        System.out.println( "Starting waiting games...");
        List<Game> waitingGames = gameRepository.findIdsByStatusAndPlayersSizeGreaterThanOrEqualTwo(Game.GameStatus.WAITING);
        System.out.println( "Found " + waitingGames.size() + " waiting games");
        waitingGames.forEach( game -> gameService.startNewHand(game.getId()));
    }
}
