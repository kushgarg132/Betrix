package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameLifecycleServiceLeaveTest {

    /** Leaving on your own turn folds first; the fold reloads a different game object, so removal must go by id. */
    @Test
    void leavingOnYourTurnRemovesYouEvenAfterFoldReloadsTheGame() {
        GameRepository repo = mock(GameRepository.class);
        GameActionService actions = mock(GameActionService.class);
        GameValidatorService validator = new GameValidatorService(repo, mock(UserRepository.class));
        GameLifecycleService service = new GameLifecycleService(
                mock(UserRepository.class), repo, validator, mock(GameEventPublisher.class), actions);

        Game game = new Game(10, 20);
        Player a = new Player("a", "a", 1000);
        Player b = new Player("b", "b", 1000);
        game.getPlayers().add(a);
        game.getPlayers().add(b);
        game.setStatus(Game.GameStatus.FLOP_BETTING);
        game.setCurrentPlayerIndex(0);

        Game[] stored = {game};
        when(repo.findById(game.getId())).thenAnswer(i -> Optional.of(stored[0]));
        doAnswer(i -> {
            stored[0] = new Game(stored[0]); // persisted state after the fold
            stored[0].getPlayers().get(0).setHasFolded(true);
            stored[0].setCurrentPlayerIndex(1);
            return null;
        }).when(actions).fold(anyString(), anyString());

        service.leaveGame(game.getId(), a.getId());

        ArgumentCaptor<Game> saved = ArgumentCaptor.forClass(Game.class);
        verify(repo).save(saved.capture());
        assertEquals(1, saved.getValue().getPlayers().size());
        assertEquals(b.getId(), saved.getValue().getPlayers().get(0).getId());
    }

    @Test
    void removingAnEarlierSeatKeepsTurnAndDealerOnTheSamePlayers() {
        Game game = new Game(10, 20);
        for (String n : new String[] {"a", "b", "c", "d"}) {
            game.getPlayers().add(new Player(n, n, 1000));
        }
        game.setCurrentPlayerIndex(2);
        game.setDealerPosition(3);
        String current = game.getPlayers().get(2).getId();
        String dealer = game.getPlayers().get(3).getId();

        game.removePlayer(game.getPlayers().get(0).getId());

        assertEquals(current, game.getPlayers().get(game.getCurrentPlayerIndex()).getId());
        assertEquals(dealer, game.getPlayers().get(game.getDealerPosition()).getId());
        assertTrue(game.getPlayers().stream().noneMatch(p -> p.getUsername().equals("a")));
    }
}
