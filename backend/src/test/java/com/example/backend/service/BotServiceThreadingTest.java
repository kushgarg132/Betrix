package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A slow Gemini call must not occupy the shared scheduler threads that fire player timeouts. */
class BotServiceThreadingTest {

    @Test
    void botTurnsRunOnTheirOwnThreadsNotTheSharedScheduler() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        BotActionService actions = mock(BotActionService.class);
        GameLifecycleService lifecycle = mock(GameLifecycleService.class);
        GameRepository repo = mock(GameRepository.class);
        BotService bots = new BotService(lifecycle, repo, actions, scheduler);

        Game game = new Game(10, 20);
        Player bot = new Player("Bot", "bot-1", 1000);
        bot.setBot(true);
        game.getPlayers().add(bot);
        game.getPlayers().add(new Player("H", "h", 1000));
        game.setCurrentPlayerIndex(0);
        when(repo.findById(game.getId())).thenReturn(java.util.Optional.of(game));
        when(lifecycle.joinGame(anyString(), anyString())).thenAnswer(i -> {
            game.getPlayers().get(0).setUsername(i.getArgument(1));
            return game;
        });
        bots.addBot(game.getId(), null);

        AtomicReference<String> ranOn = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        doAnswer(i -> {
            ranOn.set(Thread.currentThread().getName());
            done.countDown();
            return null;
        }).when(actions).takeTurn(anyString(), anyString());

        bots.onGameUpdate(game.getId(), game);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), any(Instant.class));
        task.getValue().run(); // the scheduler thread fires this
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertTrue(ranOn.get().startsWith("bot-turn-"), "ran on " + ranOn.get());
        bots.shutdown();
    }
}
