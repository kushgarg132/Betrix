package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Card;
import com.example.backend.model.Card.Rank;
import com.example.backend.model.Card.Suit;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class BotActionServiceGeminiTest {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static String raise(int amount) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"action\\\":\\\"RAISE\\\",\\\"amount\\\":" + amount + "}\"}]}}]}";
    }

    private static final String FOLD = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"action\\\":\\\"FOLD\\\",\\\"amount\\\":0}\"}]}}]}";

    private GameService gameService;
    private GameRepository repo;
    private MockRestServiceServer server;
    private RestClient client;
    private Game game;
    private Player bot;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();

        repo = mock(GameRepository.class);
        bot = new Player("Bot", "bot-1", 1000);
        bot.setBot(true);
        bot.setBotDifficulty("MEDIUM");
        bot.setHand(new ArrayList<>(List.of(new Card(Suit.SPADES, Rank.ACE), new Card(Suit.HEARTS, Rank.KING))));
        Player human = new Player("H", "h", 1000);
        game = new Game(10, 20);
        game.getPlayers().addAll(List.of(bot, human));
        game.setStatus(Game.GameStatus.FLOP_BETTING);
        game.setCurrentPlayerIndex(0);
        when(repo.findById(game.getId())).thenReturn(Optional.of(game));
    }

    private BotActionService service(String key, boolean enabled, int perMinute) {
        return new BotActionService(gameService, repo, client, key, enabled, "flash-model", "pro-model", perMinute);
    }

    @Test
    void theKeyTravelsInAHeaderNeverInTheUrl() {
        server.expect(requestTo(BASE + "flash-model:generateContent"))
                .andExpect(method(POST))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andExpect(request -> assertFalse(request.getURI().toString().contains("key="), "key in URL"))
                .andRespond(withSuccess(FOLD, MediaType.APPLICATION_JSON));

        service("secret-key", true, 30).takeTurn(game.getId(), bot.getId());

        server.verify();
        verify(gameService).fold(game.getId(), bot.getId());
    }

    @Test
    void hardBotsUseTheConfiguredHardModel() {
        bot.setBotDifficulty("HARD");
        server.expect(requestTo(BASE + "pro-model:generateContent")).andRespond(withSuccess(FOLD, MediaType.APPLICATION_JSON));

        service("secret-key", true, 30).takeTurn(game.getId(), bot.getId());

        server.verify();
    }

    @Test
    void aFailingApiFallsBackToAnActionInsteadOfStallingTheTable() {
        server.expect(requestTo(BASE + "flash-model:generateContent")).andRespond(withServerError());

        service("secret-key", true, 30).takeTurn(game.getId(), bot.getId());

        assertFalse(Mockito.mockingDetails(gameService).getInvocations().isEmpty(), "bot took no action");
    }

    @Test
    void callsBeyondTheBudgetPlayLocallyAndCostNothing() {
        server.expect(requestTo(BASE + "flash-model:generateContent")).andRespond(withSuccess(FOLD, MediaType.APPLICATION_JSON));
        BotActionService bots = service("secret-key", true, 1);

        bots.takeTurn(game.getId(), bot.getId()); // uses the one call in the budget
        bots.takeTurn(game.getId(), bot.getId()); // a second HTTP request would fail the mock server

        server.verify();
        assertTrue(Mockito.mockingDetails(gameService).getInvocations().size() >= 2);
    }

    @Test
    void noKeyOrDisabledMeansNoRequestAtAll() {
        service("", true, 30).takeTurn(game.getId(), bot.getId());
        service("secret-key", false, 30).takeTurn(game.getId(), bot.getId());

        server.verify(); // no expectations were set, so any request would already have failed
        assertTrue(Mockito.mockingDetails(gameService).getInvocations().size() >= 2);
    }

    /** An illegal raise size used to make executeAction fail and the bot fold; it should raise the legal minimum. */
    @Test
    void aRaiseBelowTheMinimumIsBumpedToTheMinimumInsteadOfBeingRefused() {
        game.setCurrentBet(20); // big blind 20, bot has nothing in
        server.expect(requestTo(BASE + "flash-model:generateContent")).andRespond(withSuccess(raise(25), MediaType.APPLICATION_JSON));

        service("secret-key", true, 30).takeTurn(game.getId(), bot.getId());

        verify(gameService).placeBet(game.getId(), bot.getId(), 40L); // current bet 20 + one big blind
    }

    @Test
    void aBigRaiseIsPlayedAsAskedAsTotalChipsInForTheRound() {
        game.setCurrentBet(20);
        game.getCurrentBettingRound().getBets().put(bot.getId(), 20L); // bot posted the big blind
        server.expect(requestTo(BASE + "flash-model:generateContent")).andRespond(withSuccess(raise(100), MediaType.APPLICATION_JSON));

        service("secret-key", true, 30).takeTurn(game.getId(), bot.getId());

        verify(gameService).placeBet(game.getId(), bot.getId(), 80L); // to 100 total = 80 more
    }
}
