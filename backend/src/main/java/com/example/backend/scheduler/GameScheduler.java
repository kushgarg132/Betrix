package com.example.backend.scheduler;

import com.example.backend.entity.Game;
import com.example.backend.repository.GameRepository;
import com.example.backend.service.GameService;
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
    
    // Configurable timing properties with defaults
    @Value("${game.scheduler.active-games-check-interval:2000}")
    private long activeGamesCheckInterval; // milliseconds
    
    @Value("${game.scheduler.idle-games-check-interval:60000}")
    private long idleGamesCheckInterval; // milliseconds
    
    @Value("${game.scheduler.min-player-timeout-check:1000}")
    private long minPlayerTimeoutCheck; // minimum milliseconds to check for player timeouts
    
    @Value("${game.scheduler.max-player-timeout-check:5000}")
    private long maxPlayerTimeoutCheck; // maximum milliseconds to check for player timeouts
    
    private final GameRepository gameRepository;
    private final GameService gameService;
    private final TaskScheduler taskScheduler;
    
    private ScheduledTaskRegistrar taskRegistrar;
    private ScheduledFuture<?> playerTimeoutTask;
    private ScheduledFuture<?> startGamesTask;
    private ScheduledFuture<?> metricsTask;
    
    private AtomicReference<Long> currentPlayerTimeoutInterval = new AtomicReference<>(5000L);
    private AtomicReference<Long> currentGameStartInterval = new AtomicReference<>(10000L);
    
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
                                   "cleanupIdleGames", "updateActionDeadlines")) {
            taskExecutionCounts.put(task, new AtomicLong(0));
            taskExecutionTimes.put(task, new AtomicLong(0));
            taskErrorCounts.put(task, new AtomicLong(0));
            taskLastExecutions.put(task, Instant.now());
        }
    }
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
        taskRegistrar.setTaskScheduler(taskScheduler);
        
        // Schedule the dynamic tasks
        schedulePlayerTimeoutTask();
        scheduleGameStartTask();
        
        // Schedule fixed tasks
        taskRegistrar.addTriggerTask(
            this::cleanupIdleGames,
            triggerContext -> new PeriodicTrigger(idleGamesCheckInterval, TimeUnit.MILLISECONDS).nextExecutionTime(triggerContext).toInstant()
        );
        
        taskRegistrar.addTriggerTask(
            this::updateActionDeadlines,
            triggerContext -> new PeriodicTrigger(activeGamesCheckInterval, TimeUnit.MILLISECONDS).nextExecutionTime(triggerContext).toInstant()
        );
        
        // Schedule a task to adjust dynamic scheduler intervals
        taskRegistrar.addTriggerTask(
            this::adjustSchedulerIntervals,
            triggerContext -> new PeriodicTrigger(30000, TimeUnit.MILLISECONDS).nextExecutionTime(triggerContext).toInstant()
        );
        
        // Schedule metrics logging
        metricsTask = taskScheduler.schedule(
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
        sb.append(String.format("Intervals: playerTimeout=%dms, gameStart=%dms\n",
                currentPlayerTimeoutInterval.get(), currentGameStartInterval.get()));
        
        logger.info(sb.toString());
    }
    
    /**
     * Dynamically schedule player timeout checks based on game activity
     */
    private void schedulePlayerTimeoutTask() {
        if (playerTimeoutTask != null && !playerTimeoutTask.isDone()) {
            playerTimeoutTask.cancel(false);
        }
        
        playerTimeoutTask = taskScheduler.schedule(
            this::handlePlayerTimeouts,
            triggerContext -> new PeriodicTrigger(currentPlayerTimeoutInterval.get(), TimeUnit.MILLISECONDS)
                .nextExecutionTime(triggerContext).toInstant()
        );
    }
    
    /**
     * Dynamically schedule game start checks based on waiting games
     */
    private void scheduleGameStartTask() {
        if (startGamesTask != null && !startGamesTask.isDone()) {
            startGamesTask.cancel(false);
        }
        
        startGamesTask = taskScheduler.schedule(
            this::startWaitingGames,
            triggerContext -> new PeriodicTrigger(currentGameStartInterval.get(), TimeUnit.MILLISECONDS)
                .nextExecutionTime(triggerContext).toInstant()
        );
    }
    
    /**
     * Adjust scheduler intervals based on current game activity
     */
    private void adjustSchedulerIntervals() {
        try {
            // Check active games count
            List<Game> activeGames = gameRepository.findActiveGames();
            int activeGamesCount = activeGames.size();
            
            // Find games with imminent player timeouts
            Instant now = Instant.now();
            List<Game> timeoutGames = gameRepository.findGamesWithImminentPlayerActionTimeout(
                    now, now.plusSeconds(10)); // Games with timeout in next 10 seconds
            int imminentTimeouts = timeoutGames.size();
            
            // Find waiting games
            List<Game> waitingGames = gameRepository.findGamesReadyForAutoStart();
            int waitingGamesCount = waitingGames.size();
            
            // Get counts by status for better metrics
            long waitingCount = gameRepository.countGamesByStatus(Game.GameStatus.WAITING);
            long preFlopCount = gameRepository.countGamesByStatus(Game.GameStatus.PRE_FLOP_BETTING);
            long flopCount = gameRepository.countGamesByStatus(Game.GameStatus.FLOP_BETTING);
            long turnCount = gameRepository.countGamesByStatus(Game.GameStatus.TURN_BETTING);
            long riverCount = gameRepository.countGamesByStatus(Game.GameStatus.RIVER_BETTING);
            
            // Adjust player timeout check interval - faster checks if there are active games with imminent timeouts
            long newTimeoutInterval;
            if (imminentTimeouts > 0) {
                // More imminent timeouts = more frequent checks
                newTimeoutInterval = Math.max(minPlayerTimeoutCheck, 
                                     Math.min(maxPlayerTimeoutCheck, 5000 / (imminentTimeouts + 1)));
            } else if (activeGamesCount > 0) {
                // If we have active games but no imminent timeouts, check at a moderate pace
                newTimeoutInterval = Math.max(minPlayerTimeoutCheck, 
                                     Math.min(maxPlayerTimeoutCheck, 3000));
            } else {
                // No active games, check less frequently
                newTimeoutInterval = maxPlayerTimeoutCheck;
            }
            
            // Adjust game start interval - faster checks if there are waiting games ready to start
            long newGameStartInterval;
            if (waitingGamesCount > 5) {
                // Many waiting games, check very frequently
                newGameStartInterval = 1000;
            } else if (waitingGamesCount > 0) {
                // Some waiting games, check frequently
                newGameStartInterval = 2000;
            } else if (waitingCount > 0) {
                // Games waiting but not enough players yet, check occasionally
                newGameStartInterval = 5000;
            } else {
                // No waiting games at all, check infrequently
                newGameStartInterval = 10000;
            }
            
            // Only reschedule if intervals have changed significantly (more than 20%)
            if (Math.abs(newTimeoutInterval - currentPlayerTimeoutInterval.get()) > 
                    (currentPlayerTimeoutInterval.get() * 0.2)) {
                currentPlayerTimeoutInterval.set(newTimeoutInterval);
                schedulePlayerTimeoutTask();
                logger.debug("Adjusted player timeout check interval to {}ms", newTimeoutInterval);
            }
            
            if (Math.abs(newGameStartInterval - currentGameStartInterval.get()) > 
                    (currentGameStartInterval.get() * 0.2)) {
                currentGameStartInterval.set(newGameStartInterval);
                scheduleGameStartTask();
                logger.debug("Adjusted game start check interval to {}ms", newGameStartInterval);
            }
            
            logger.debug("Scheduler status: activeGames={}, imminentTimeouts={}, waitingGames={}, " +
                         "gamesByStatus=[waiting={}, preFlop={}, flop={}, turn={}, river={}], " +
                         "timeoutInterval={}ms, startInterval={}ms",
                         activeGamesCount, imminentTimeouts, waitingGamesCount,
                         waitingCount, preFlopCount, flopCount, turnCount, riverCount,
                         currentPlayerTimeoutInterval.get(), currentGameStartInterval.get());
            
        } catch (Exception e) {
            logger.error("Error adjusting scheduler intervals: {}", e.getMessage(), e);
        }
    }

    /**
     * Auto-start games that are waiting and have enough players
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
    
    /**
     * Handle player timeouts by auto-folding
     */
    private void handlePlayerTimeouts() {
        logger.debug("Checking for player action timeouts...");
        String taskName = "handlePlayerTimeouts";
        Instant start = Instant.now();
        taskLastExecutions.put(taskName, start);
        
        try {
            List<Game> timeoutGames = gameRepository.findGamesWithPlayerActionTimeout(Instant.now());
            
            if (!timeoutGames.isEmpty()) {
                logger.info("Found {} games with player action timeouts", timeoutGames.size());
                
                timeoutGames.forEach(game -> {
                    try {
                        // Find current player
                        if (game.getCurrentPlayerIndex() >= 0 && 
                            game.getCurrentPlayerIndex() < game.getPlayers().size()) {
                            
                            String playerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
                            gameService.fold(game.getId(), playerId);
                            logger.info("Auto-folded for player {} in game {}", playerId, game.getId());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to handle timeout for game {}: {}", game.getId(), e.getMessage(), e);
                    }
                });
            }
            
            // Update metrics
            taskExecutionCounts.get(taskName).incrementAndGet();
            taskExecutionTimes.get(taskName).addAndGet(Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            logger.error("Error in handlePlayerTimeouts scheduler: {}", e.getMessage(), e);
            taskErrorCounts.get(taskName).incrementAndGet();
        }
    }
    
    /**
     * Clean up idle games
     */
    private void cleanupIdleGames() {
        logger.debug("Checking for idle games...");
        String taskName = "cleanupIdleGames";
        Instant start = Instant.now();
        taskLastExecutions.put(taskName, start);
        
        try {
            LocalDateTime idleThreshold = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(Game.DEFAULT_GAME_IDLE_TIMEOUT_MINUTES);
            List<Game> idleGames = gameRepository.findIdleGames(idleThreshold);
            
            if (!idleGames.isEmpty()) {
                logger.info("Found {} idle games to clean up", idleGames.size());
                
                idleGames.forEach(game -> {
                    try {
                        // If game is in progress, auto-fold current player and mark as finished
                        if (game.getStatus() != Game.GameStatus.WAITING && 
                            game.getStatus() != Game.GameStatus.FINISHED) {
                            
                            if (game.getCurrentPlayerIndex() >= 0 && 
                                game.getCurrentPlayerIndex() < game.getPlayers().size()) {
                                
                                String playerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
                                gameService.fold(game.getId(), playerId);
                            }
                            
                            // Force game to finished state
                            game.setStatus(Game.GameStatus.FINISHED);
                            gameRepository.save(game);
                        }
                        
                        logger.info("Cleaned up idle game: {}", game.getId());
                    } catch (Exception e) {
                        logger.error("Failed to clean up idle game {}: {}", game.getId(), e.getMessage(), e);
                    }
                });
            }
            
            // Update metrics
            taskExecutionCounts.get(taskName).incrementAndGet();
            taskExecutionTimes.get(taskName).addAndGet(Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            logger.error("Error in cleanupIdleGames scheduler: {}", e.getMessage(), e);
            taskErrorCounts.get(taskName).incrementAndGet();
        }
    }
    
    /**
     * Update action deadlines for active games
     */
    private void updateActionDeadlines() {
        logger.debug("Updating action deadlines for active games...");
        String taskName = "updateActionDeadlines";
        Instant start = Instant.now();
        taskLastExecutions.put(taskName, start);
        
        try {
            gameService.updateAllGameActionDeadlines();
            
            // Update metrics
            taskExecutionCounts.get(taskName).incrementAndGet();
            taskExecutionTimes.get(taskName).addAndGet(Duration.between(start, Instant.now()).toMillis());
        } catch (Exception e) {
            logger.error("Error in updateActionDeadlines scheduler: {}", e.getMessage(), e);
            taskErrorCounts.get(taskName).incrementAndGet();
        }
    }
}
