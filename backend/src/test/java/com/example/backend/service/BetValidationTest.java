package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** Big blind 20, someone has opened to 100. */
class BetValidationTest {

    private GameValidatorService validator;
    private Game game;
    private Player p;

    @BeforeEach
    void setUp() {
        validator = new GameValidatorService(mock(GameRepository.class), mock(UserRepository.class));
        game = new Game(10, 20);
        p = new Player("p", "p", 1000);
        game.getPlayers().add(p);
        game.setCurrentBet(100);
    }

    @Test
    void callingAndRaisingByAtLeastABigBlindAreAllowed() {
        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 100)); // call
        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 120)); // minimum raise: to 120
        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 500));
    }

    @Test
    void aRaiseSmallerThanABigBlindIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> validator.validatePlayerBetAmount(game, p, 101));
        assertThrows(IllegalArgumentException.class, () -> validator.validatePlayerBetAmount(game, p, 119));
    }

    @Test
    void theRaiseIsMeasuredOnWhatThePlayerAlreadyHasIn() {
        game.getCurrentBettingRound().getBets().put(p.getId(), 100L); // already in for 100

        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 0)); // check
        assertThrows(IllegalArgumentException.class, () -> validator.validatePlayerBetAmount(game, p, 5)); // to 105
        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 20)); // to 120
    }

    @Test
    void goingAllInForLessThanAFullRaiseIsAllowed() {
        p.setChips(110); // can only get to 110 total: a short all-in raise

        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 110));
    }

    @Test
    void aShortAllInCallIsAllowedAndBetsOverTheStackOrBelowTheCallAreNot() {
        p.setChips(60);
        assertDoesNotThrow(() -> validator.validatePlayerBetAmount(game, p, 60)); // all-in for less than the call

        assertThrows(IllegalArgumentException.class, () -> validator.validatePlayerBetAmount(game, p, 61));
        p.setChips(1000);
        assertThrows(IllegalArgumentException.class, () -> validator.validatePlayerBetAmount(game, p, -5));
        assertThrows(IllegalArgumentException.class, () -> validator.validatePlayerBetAmount(game, p, 50)); // below the call
    }
}
