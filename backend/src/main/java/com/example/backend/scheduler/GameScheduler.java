package com.example.backend.scheduler;

import com.example.backend.entity.Game;
import com.example.backend.repository.GameRepository;
import com.example.backend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class GameScheduler implements SchedulingConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(GameScheduler.class);

    @Value("${game.scheduler.round-end-delay:5000}")
    private long roundEndDelay; // milliseconds to wait after round end before starting new hand
    
    @Value("${game.scheduler.player-timeout-delay:3000}")
    private long playerTimeoutDelay; // milliseconds to wait before auto-folding a player
    
    private final GameRepository gameRepository;
    private final GameService gameService;
    private final TaskScheduler taskScheduler;
    
    private final AtomicReference<Long> currentPlayerTimeoutInterval = new AtomicReference<>(5000L);
    private final AtomicReference<Long> currentGameStartInterval = new AtomicReference<>(10000L);
    
    // Track games with scheduled next hand starts
    private final Map<String, ScheduledFuture<?>> scheduledGameStarts = new ConcurrentHashMap<>();
    
    // Track players with scheduled timeout actions
    private final Map<String, ScheduledFuture<?>> scheduledPlayerTimeouts = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final Map<String, AtomicLong> taskExecutionCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> taskExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> taskErrorCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> taskLastExecutions = new ConcurrentHashMap<>();
    
    public GameScheduler(GameRepository gameRepository, @Lazy GameService gameService, TaskScheduler taskScheduler) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.taskScheduler = taskScheduler;
    }
    
    @PostConstruct
    public void initMetrics() {
        // Initialize metrics for each task
        for (String task : List.of("startWaitingGames", "handlePlayerTimeouts", 
                                   "cleanupIdleGames", "updateActionDeadlines", 
                                   "scheduleNextHand", "schedulePlayerTimeout")) {
            taskExecutionCounts.put(task, new AtomicLong(0));
            taskExecutionTimes.put(task, new AtomicLong(0));
            taskErrorCounts.put(task, new AtomicLong(0));
            taskLastExecutions.put(task, Instant.now());
        }
    }
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler);

        taskScheduler.schedule(
                this::startWaitingGames,
                triggerContext -> new PeriodicTrigger(currentGameStartInterval.get(), TimeUnit.MILLISECONDS)
                        .nextExecutionTime(triggerContext).toInstant()
        );
        
        // Schedule metrics logging
        taskScheduler.schedule(
            this::logMetrics,
            triggerContext -> new PeriodicTrigger(60000, TimeUnit.MILLISECONDS).nextExecutionTime(triggerContext).toInstant()
        );
    }
    
    /**
     * Logs performance metrics
     */
    private void logMetrics() {
        StringBuilder sb = new StringBuilder("Scheduler Metrics:\n");
        
        // Format each task's metrics
        for (String task : taskExecutionCounts.keySet()) {
            long count = taskExecutionCounts.get(task).get();
            long totalTimeMs = taskExecutionTimes.get(task).get();
            long avgTimeMs = count > 0 ? totalTimeMs / count : 0;
            long errors = taskErrorCounts.get(task).get();
            Instant lastExecution = taskLastExecutions.get(task);
            
            sb.append(String.format("  %s: executions=%d, avgTime=%dms, errors=%d, lastRun=%s\n",
                    task, count, avgTimeMs, errors, 
                    Duration.between(lastExecution, Instant.now()).toSeconds() + "s ago"));
        }
        
        // Add scheduler configuration
        sb.append(String.format("Intervals: playerTimeout=%dms, gameStart=%dms, roundEndDelay=%dms, playerTimeoutDelay=%dms\n",
                currentPlayerTimeoutInterval.get(), currentGameStartInterval.get(), roundEndDelay, playerTimeoutDelay));
        
        // Add info about scheduled game starts and player timeouts
        sb.append(String.format("Scheduled game starts: %d, Scheduled player timeouts: %d\n", 
                scheduledGameStarts.size(), scheduledPlayerTimeouts.size()));
        
        logger.info(sb.toString());
    }
    
    /**
     * Schedule the next hand for a game after the round end delay
     * @param gameId The ID of the game to schedule next hand for
     */
    public void scheduleNextHand(String gameId) {
        String taskName = "scheduleNextHand";
        Instant start = Instant.now();
        taskLastExecutions.put(taskName, start);
        
        try {
            logger.info("Scheduling next hand for game {} after {}ms delay", gameId, roundEndDelay);
            
            // Cancel any existing scheduled start for this game
            ScheduledFuture<?> existingTask = scheduledGameStarts.remove(gameId);
            if (existingTask != null && !existingTask.isDone()) {
                existingTask.cancel(false);
            }
            
            // Schedule the new hand start after delay
            ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                try {
                    Game game = gameRepository.findById(gameId).orElse(null);
                    if (game != null && game.getStatus() == Game.GameStatus.WAITING) {
                        gameService.startNewHand(gameId);
                        logger.info("Started new hand for game {} after delay", gameId);
                    } else {
                        logger.debug("Skipping scheduled start for game {}: game not found or not in WAITING status", gameId);
                    }
                } catch (Exception e) {
                    logger.error("Error starting new hand for game {}: {}", gameId, e.getMessage(), e);
                } finally {
                    // Remove from tracking map when done
                    scheduledGameStarts.remove(gameId);
                }
            }, Instant.now().plusMillis(roundEndDelay));
            
            // Store the future for potential cancellation
            scheduledGameStarts.put(gameId, future);
            
            // Update metrics
            taskExecutionCounts.get(taskName).incrementAndGet();
            taskExecutionTimes.get(taskName).addAndGet(Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            logger.error("Error scheduling next hand for game {}: {}", gameId, e.getMessage(), e);
            taskErrorCounts.get(taskName).incrementAndGet();
        }
    }

    /**
     * Schedule a player timeout action with delay
     * @param gameId The ID of the game
     * @param playerId The ID of the player who timed out
     */
    public void schedulePlayerTimeout(String gameId, String playerId) {
        String taskName = "schedulePlayerTimeout";
        Instant start = Instant.now();
        taskLastExecutions.put(taskName, start);

        try {
            logger.info("Scheduling timeout for player {} in game {} after {}ms delay", playerId, gameId, playerTimeoutDelay);

            // Create a unique key for this timeout
            String timeoutKey = gameId + ":" + playerId;

            // Cancel any existing scheduled timeout for this player in this game
            ScheduledFuture<?> existingTask = scheduledPlayerTimeouts.remove(timeoutKey);
            if (existingTask != null && !existingTask.isDone()) {
                existingTask.cancel(false);
                logger.debug("Cancelled existing timeout task for player {} in game {}", playerId, gameId);
            }

            // Schedule the player timeout after delay
            ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                handlePlayerTimeOut(gameId, playerId);
            }, Instant.now().plusMillis(playerTimeoutDelay));

            // Store the future for potential cancellation
            scheduledPlayerTimeouts.put(timeoutKey, future);

            // Update metrics
            taskExecutionCounts.get(taskName).incrementAndGet();
            taskExecutionTimes.get(taskName).addAndGet(Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            logger.error("Error scheduling timeout for player {} in game {}: {}", playerId, gameId, e.getMessage(), e);
            taskErrorCounts.get(taskName).incrementAndGet();
        }
    }

    private void handlePlayerTimeOut(String gameId, String playerId) {
        // Create a unique key for this timeout
        String timeoutKey = gameId + ":" + playerId;
        try {
            Game game = gameRepository.findById(gameId).orElse(null);
            if (game != null && game.getStatus() != Game.GameStatus.WAITING) {
                // Double check that this player is still the current player
                if (game.isPlayersTurn(playerId)) {
                    // Auto-fold the player
                    gameService.fold(gameId, playerId);
                    logger.info("Auto-folded player {} in game {} after timeout delay", playerId, gameId);
                } else {
                    logger.debug("Skipping timeout for player {} in game {}: no longer their turn", playerId, gameId);
                }
            } else {
                logger.debug("Skipping timeout for player {} in game {}: game not found or in WAITING status", playerId, gameId);
            }
        } catch (Exception e) {
            logger.error("Error handling timeout for player {} in game {}: {}", playerId, gameId, e.getMessage(), e);
        } finally {
            // Remove from tracking map when done
            scheduledPlayerTimeouts.remove(timeoutKey);
        }
    }

    public void cancelPlayerTimeout(String gameId, String playerId) {
        // Create a unique key for this timeout
        String timeoutKey = gameId + ":" + playerId;
        ScheduledFuture<?> existingTask = scheduledPlayerTimeouts.remove(timeoutKey);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
            logger.debug("Cancelled scheduled timeout for {}", timeoutKey);
        }
    }

    /**
     * Start waiting games that are ready for auto-start
     */
    private void startWaitingGames() {
        logger.debug("Checking for waiting games to start...");
        String taskName = "startWaitingGames";
        Instant start = Instant.now();
        taskLastExecutions.put(taskName, start);
        
        try {
            List<Game> waitingGames = gameRepository.findGamesReadyForAutoStart();
            
            if (!waitingGames.isEmpty()) {
                logger.info("Found {} waiting games ready to auto-start", waitingGames.size());
                
                waitingGames.forEach(game -> {
                    try {
                        // Skip games that already have a scheduled start
                        if (scheduledGameStarts.containsKey(game.getId())) {
                            logger.debug("Skipping game {} as it already has a scheduled start", game.getId());
                            return;
                        }
                        
                        gameService.startNewHand(game.getId());
                        logger.info("Auto-started game: {}", game.getId());
                    } catch (Exception e) {
                        logger.error("Failed to auto-start game {}: {}", game.getId(), e.getMessage(), e);
                    }
                });
            }
            
            // Update metrics
            taskExecutionCounts.get(taskName).incrementAndGet();
            taskExecutionTimes.get(taskName).addAndGet(Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            logger.error("Error in startWaitingGames scheduler: {}", e.getMessage(), e);
            taskErrorCounts.get(taskName).incrementAndGet();
        }
    }
}
