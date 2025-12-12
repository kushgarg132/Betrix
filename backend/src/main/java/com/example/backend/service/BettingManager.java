package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.event.CardsDealtEvent;
import com.example.backend.event.GameEndedEvent;
import com.example.backend.model.*;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.event.RoundStartedEvent;
import com.example.backend.scheduler.GameScheduler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;

@Service
@RequiredArgsConstructor
public class BettingManager {
    private static final Logger logger = LoggerFactory.getLogger(BettingManager.class);

    private final GameEventPublisher eventPublisher;
    private final HandEvaluator handEvaluator;
    private final GameScheduler gameScheduler;
    private final List<Game.PlayerAction> notAllowedStatus = List.of(Game.PlayerAction.NONE,
            Game.PlayerAction.SMALL_BLIND, Game.PlayerAction.BIG_BLIND);

    public void startNewBettingRound(Game game) {
        logger.info("Starting a new betting round for game with ID: {}", game.getId());
        game.setupNextRound();

        BettingRound.RoundType roundType = BettingRound.RoundType.PRE_FLOP;

        switch (game.getStatus()) {
            case STARTING:
                setupPreFlopBetting(game);
                break;
            case PRE_FLOP_BETTING:
                setupFlopBetting(game);
                roundType = BettingRound.RoundType.FLOP;
                break;
            case FLOP_BETTING:
                setupTurnBetting(game);
                roundType = BettingRound.RoundType.TURN;
                break;
            case TURN_BETTING:
                setupRiverBetting(game);
                roundType = BettingRound.RoundType.RIVER;
                break;
        }

        String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
        gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

        // Publish event for round started
        eventPublisher.publishEvent(new RoundStartedEvent(game.getId(), game, roundType));

        logger.debug("Betting round started. Updated game state: {}", game);
    }

    private void setupPreFlopBetting(Game game) {
        // Post small blind
        Player smallBlind = findPlayerByUsername(game, game.getSmallBlindUserId());
        placeBet(game, smallBlind, game.getSmallBlindAmount(), Game.PlayerAction.SMALL_BLIND);

        // Post big blind
        Player bigBlind = findPlayerByUsername(game, game.getBigBlindUserId());
        placeBet(game, bigBlind, game.getBigBlindAmount(), Game.PlayerAction.BIG_BLIND);

        game.setCurrentBet(game.getBigBlindAmount());
        game.setStatus(Game.GameStatus.PRE_FLOP_BETTING);
    }

    private void setupFlopBetting(Game game) {
        game.setStatus(Game.GameStatus.FLOP_BETTING);

        List<Card> communityCards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            communityCards.add(game.getDeck().drawCard());
        }
        game.setCommunityCards(communityCards);

        // Publish flop cards dealt event
        eventPublisher.publishEvent(new CardsDealtEvent(
                game.getId(),
                CardsDealtEvent.DealType.FLOP,
                new ArrayList<>(communityCards)));
    }

    private void setupTurnBetting(Game game) {
        game.setStatus(Game.GameStatus.TURN_BETTING);

        // Add one card to the community cards
        List<Card> communityCards = game.getCommunityCards();
        Card turnCard = game.getDeck().drawCard();
        communityCards.add(turnCard);
        game.setCommunityCards(communityCards);

        // Publish turn card dealt event
        eventPublisher.publishEvent(new CardsDealtEvent(
                game.getId(),
                CardsDealtEvent.DealType.TURN,
                new ArrayList<>(communityCards)));
    }

    private void setupRiverBetting(Game game) {
        game.setStatus(Game.GameStatus.RIVER_BETTING);

        // Add one card to the community cards
        List<Card> communityCards = game.getCommunityCards();
        Card riverCard = game.getDeck().drawCard();
        communityCards.add(riverCard);
        game.setCommunityCards(communityCards);

        // Publish river card dealt event
        eventPublisher.publishEvent(new CardsDealtEvent(
                game.getId(),
                CardsDealtEvent.DealType.RIVER,
                new ArrayList<>(communityCards)));
    }

    /**
     * Initiates the sequence of revealing community cards when players are all-in.
     * Instead of a while loop, we schedule the next step to allow for UI delay.
     */
    private void initiateAllInSequence(Game game) {
        logger.info("All active players are all-in. Initiating all-in sequence for game: {}", game.getId());

        // Cancel any existing player timeouts as they don't apply when all-in
        String playerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
        gameScheduler.cancelPlayerTimeout(game.getId(), playerId);

        // Schedule the next step immediately (or with small delay)
        gameScheduler.scheduleAllInAction(game.getId());
    }

    /**
     * Processes a single step of the all-in sequence.
     * Called by the GameService (triggered by Scheduler).
     */
    public void processAllInRound(Game game) {
        logger.info("Processing all-in round for game: {}. Current Status: {}", game.getId(), game.getStatus());

        if (game.getStatus() == Game.GameStatus.RIVER_BETTING) {
            // We reached the end, evaluate hands
            evaluateHandAndAwardPot(game);
        } else {
            // Advance to next round (FLOP -> TURN -> RIVER)
            startNewBettingRound(game);

            // Schedule the NEXT step
            gameScheduler.scheduleAllInAction(game.getId());
        }
    }

    public void placeBet(Game game, Player player, double amount, Game.PlayerAction action) {
        logger.info("Player '{}' is placing a bet of {} in game with ID: {}", player.getUsername(), amount,
                game.getId());
        double betPlacedAmount = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername(), 0.0);
        double betPlacedTotal = betPlacedAmount + amount;
        boolean isAllIn = false;

        // Check for Blinds or All-in
        if (action == Game.PlayerAction.BIG_BLIND || action == Game.PlayerAction.SMALL_BLIND) {
            game.getLastActions().put(player.getUsername(), action);
        } else if (amount == player.getChips()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.ALL_IN);
            isAllIn = true;
        } else if (betPlacedTotal == game.getCurrentBet()) {
            if (amount == 0)
                game.getLastActions().put(player.getUsername(), Game.PlayerAction.CHECK);
            else
                game.getLastActions().put(player.getUsername(), Game.PlayerAction.CALL);
        } else if (betPlacedTotal > game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.RAISE);
        }

        game.setCurrentBet(max(betPlacedTotal, game.getCurrentBet()));

        player.placeBet(amount);

        // Handle pot management
        if (isAllIn) {
            handleAllInBet(game, player, amount);
        } else {
            // Regular bet goes to main pot
            game.addToPot(amount);
        }

        game.getCurrentBettingRound().getPlayerBets().put(player.getUsername(), betPlacedTotal);

        // Move to next player after bet
        game.moveToNextPlayer();

        logger.debug("Bet placed. Updated game state: {}", game);
    }

    public void fold(Game game, Player player) {
        logger.info("Player '{}' is folding in game with ID: {}", player.getUsername(), game.getId());

        // Mark player as folded and record action
        player.setHasFolded(true);
        player.setActive(false);
        game.getLastActions().put(player.getUsername(), Game.PlayerAction.FOLD);

        // Move to next player after fold
        game.moveToNextPlayer();

        logger.debug("Player '{}' folded. Updated game state: {}", player.getUsername(), game);
    }

    /**
     * Handles the creation of side pots when a player goes all-in
     */
    /**
     * Handles the creation of side pots when a player goes all-in
     * Simplified: Now explicitly delegates to updatePotAmounts at end of round.
     * This method simply adds to main pot temporarily.
     */
    private void handleAllInBet(Game game, Player allInPlayer, double amount) {
        logger.info("Player '{}' is all-in with {} chips", allInPlayer.getUsername(), amount);
        // Just add to main pot for now; will be redistributed correctly in
        // updatePotAmounts
        game.addToPot(amount);
    }

    public void handleCurrentBettingRound(Game game, String playerId) {
        // Check if betting round is complete
        if (isBettingRoundComplete(game)) {
            gameScheduler.cancelPlayerTimeout(game.getId(), playerId);

            // Update pot amounts before moving to the next round
            updatePotAmounts(game);

            if (getActivePlayerCount(game) <= 1 || game.getStatus() == Game.GameStatus.RIVER_BETTING) {
                // Game will be ended by the service that calls this method
                evaluateHandAndAwardPot(game);
                // Instead of immediately setting status to WAITING, the game will be scheduled
                // for restart
                // after a delay in evaluateHandAndAwardPot
            } else if (areAllActivePlayersAllIn(game)) {
                // If all active players are all-in, initiate the scheduled sequence
                initiateAllInSequence(game);
            } else {
                startNewBettingRound(game);
            }
        }
    }

    public boolean isBettingRoundComplete(Game game) {
        logger.info("Checking if betting round is complete for game with ID: {}", game.getId());
        int activePlayerCount = getActivePlayerCount(game);
        if (activePlayerCount <= 1) {
            logger.debug("Betting round complete status: true");
            return true;
        }

        // Check if all active players have acted and bet the same amount
        double targetBet = game.getCurrentBet();
        for (Player player : game.getPlayers()) {
            if (!player.isActive() || player.isHasFolded())
                continue;

            Double playerBet = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername(), 0.0);
            Game.PlayerAction lastAction = game.getLastActions().get(player.getUsername());

            if (notAllowedStatus.contains(lastAction) ||
                    (playerBet < targetBet && player.getChips() > 0 && lastAction != Game.PlayerAction.ALL_IN)) {
                logger.debug("Betting round complete status: false");
                return false;
            }
        }

        logger.debug("Betting round complete status: true");
        return true;
    }

    public void evaluateHandAndAwardPot(Game game) {
        logger.info("Evaluating hands and awarding pot for game with ID: {}", game.getId());

        game.setCurrentPlayerIndex(-1);
        game.setStatus(Game.GameStatus.SHOWDOWN);

        // Only evaluate if there are players still in the hand
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(p -> (p.isActive() || p.isAllIn()) && !p.isHasFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            logger.warn("No active players, unusual case");
            return;
        }

        if (activePlayers.size() == 1) {
            // Single player gets all pots
            Player winner = activePlayers.get(0);
            winner.awardPot(game.getPot());

            // Evaluate the winner's hand if they have cards
            if (!winner.getHand().isEmpty() && !game.getCommunityCards().isEmpty()) {
                HandResult result = handEvaluator.evaluateHand(
                        winner.getHand(),
                        game.getCommunityCards());
                winner.setBestHand(result);
            }

            // Publish game ended event
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    List.of(winner),
                    winner.getBestHand() // Use the winner's best hand
            ));

            // Set game status to WAITING and schedule next hand after delay
            game.setStatus(Game.GameStatus.WAITING);
            gameScheduler.scheduleNextHand(game.getId());

            logger.debug("Single player awarded pot: {}", winner);
            return;
        }

        try {
            for (Player player : activePlayers) {
                HandResult result = handEvaluator.evaluateHand(
                        player.getHand(),
                        game.getCommunityCards());
                player.setBestHand(result);
                logger.debug("Player {} hand evaluated as: {}", player.getUsername(), result.getRank());
            }

            // Process each pot separately
            List<Pot> allPots = new ArrayList<>(game.getPots());
            List<Player> allWinners = new ArrayList<>();
            Map<Player, Double> winningsMap = new HashMap<>();

            for (int potIndex = 0; potIndex < allPots.size(); potIndex++) {
                Pot pot = allPots.get(potIndex);
                double potAmount = pot.getAmount();

                if (potAmount <= 0) {
                    logger.debug("Skipping pot {} with zero amount", potIndex);
                    continue;
                }

                // Find eligible players for this pot
                List<Player> eligiblePlayers = activePlayers.stream()
                        .filter(p -> pot.getEligiblePlayerIds().isEmpty() || pot.isPlayerEligible(p.getId()))
                        .toList();

                if (eligiblePlayers.isEmpty()) {
                    logger.warn("No eligible players for pot {}, unusual case", potIndex);
                    continue;
                }

                logger.debug("Pot {} has {} eligible players and amount {}",
                        potIndex, eligiblePlayers.size(), potAmount);

                // Find the best hand among eligible players
                HandResult bestResult = null;
                for (Player player : eligiblePlayers) {
                    HandResult result = player.getBestHand();
                    if (bestResult == null || compareHandResults(result, bestResult) > 0) {
                        bestResult = result;
                    }
                }

                // Find all winners for this pot
                List<Player> potWinners = new ArrayList<>();
                for (Player player : eligiblePlayers) {
                    HandResult result = player.getBestHand();
                    if (compareHandResults(result, bestResult) == 0) {
                        potWinners.add(player);
                    }
                }

                // Split pot among winners
                double winAmount = potAmount / potWinners.size();
                for (Player winner : potWinners) {
                    // Track winnings before awarding to avoid modifying player state
                    winningsMap.merge(winner, winAmount, Double::sum);

                    if (!allWinners.contains(winner)) {
                        allWinners.add(winner);
                    }
                }

                logger.debug("Pot {} ({}) split among {} winners: {}",
                        potIndex,
                        potAmount,
                        potWinners.size(),
                        potWinners.stream().map(Player::getUsername).collect(Collectors.joining(", ")));
            }

            // Now award all accumulated winnings to players
            for (Map.Entry<Player, Double> entry : winningsMap.entrySet()) {
                Player winner = entry.getKey();
                double winAmount = entry.getValue();
                winner.awardPot(winAmount);

                // Set the amount won in the player object for UI display
                winner.setLastWinAmount(winAmount);

                logger.debug("Player {} awarded total of {}", winner.getUsername(), winAmount);
            }

            // Get the best hand from all winners (for display purposes)
            HandResult bestOverallHand = null;
            for (Player winner : allWinners) {
                HandResult winnerHand = winner.getBestHand();
                if (bestOverallHand == null || (winnerHand != null &&
                        compareHandResults(winnerHand, bestOverallHand) > 0)) {
                    bestOverallHand = winnerHand;
                }
            }

            // Publish game ended event with all winners
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    allWinners,
                    bestOverallHand // Use the best overall hand for display
            ));

            // Set game status to WAITING and schedule next hand after delay
            game.setStatus(Game.GameStatus.WAITING);
            gameScheduler.scheduleNextHand(game.getId());

        } catch (Exception e) {
            logger.error("Error evaluating hands: {}", e.getMessage(), e);
            // In case of error, still set the game to WAITING but don't schedule next hand
            game.setStatus(Game.GameStatus.WAITING);
        }
    }

    // Compare two hand results
    private int compareHandResults(HandResult result1, HandResult result2) {
        // First compare hand ranks
        int rankComparison = result1.getRank().ordinal() - result2.getRank().ordinal();
        if (rankComparison != 0) {
            return rankComparison;
        }

        // If ranks are the same, compare high cards
        List<Card> highCards1 = result1.getHighCards();
        List<Card> highCards2 = result2.getHighCards();

        int minSize = Math.min(highCards1.size(), highCards2.size());

        for (int i = 0; i < minSize; i++) {
            int valueComparison = highCards1.get(i).getRank().getValue() -
                    highCards2.get(i).getRank().getValue();
            if (valueComparison != 0) {
                return valueComparison;
            }
        }

        // If we get here, the hands are identical in rank
        return 0;
    }

    public int getActivePlayerCount(Game game) {
        return (int) game.getPlayers().stream()
                .filter(Player::isActive)
                .count();
    }

    private Player findPlayerByUsername(Game game, String playerUsername) {
        return game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(playerUsername))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    /**
     * Updates the pot amounts at the end of a betting round
     * This ensures that all bets are properly distributed to the appropriate pots
     */
    /**
     * Updates the pot amounts at the end of a betting round
     * Use an incremental approach to correctly handle side pots across multiple
     * rounds
     */
    private void updatePotAmounts(Game game) {
        logger.info("Updating pot amounts for game with ID: {}", game.getId());

        // 1. Snapshot current round bets
        Map<String, Double> currentRoundBets = new HashMap<>(game.getCurrentBettingRound().getPlayerBets());

        // 2. Identify active players for this calculation (anyone who bet > 0 this
        // round)
        List<Player> contributingPlayers = game.getPlayers().stream()
                .filter(p -> currentRoundBets.getOrDefault(p.getUsername(), 0.0) > 0)
                .collect(Collectors.toList());

        if (contributingPlayers.isEmpty()) {
            return;
        }

        // --- Standard Side Pot Distribution ---

        // distinct positive bet amounts sorted
        List<Double> betLevels = currentRoundBets.values().stream()
                .filter(b -> b > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        double prevLevel = 0.0;
        for (Double level : betLevels) {
            double contribution = level - prevLevel;
            double levelTotal = 0;
            Set<String> levelEligible = new HashSet<>();

            for (Player p : game.getPlayers()) {
                double pBet = currentRoundBets.getOrDefault(p.getUsername(), 0.0);
                if (pBet >= level) {
                    levelTotal += contribution;
                    // Player is eligible if they are in the pot (not folded)
                    // Note: if they are all-in, they are still eligible for this level if they
                    // contributed
                    if (!p.isHasFolded()) {
                        levelEligible.add(p.getId());
                    }
                } else if (pBet > prevLevel) {
                    // Partial contribution from a player between levels?
                    // This implies pBet is not in betLevels list, which is impossible as betLevels
                    // contains ALL distinct bets.
                }
            }

            if (levelTotal > 0) {
                addToCorrectPot(game, levelTotal, levelEligible);
            }
            prevLevel = level;
        }
    }

    private void addToCorrectPot(Game game, double amount, Set<String> eligiblePlayers) {
        if (game.getPots().isEmpty()) {
            game.getPots().add(new Pot(0));
        }

        Pot currentPot = game.getPots().get(game.getPots().size() - 1);
        Set<String> currentEligible = currentPot.getEligiblePlayerIds();

        // If the current pot is empty (amount 0) and has no specific eligibility (or we
        // just created it),
        // we can use it.
        // Wait, 'new Pot(0)' creates empty set??
        // Pot constructor: this.eligiblePlayerIds = new HashSet<>();

        if (currentPot.getAmount() == 0 && currentEligible.isEmpty()) {
            currentPot.addAmount(amount);
            eligiblePlayers.forEach(currentPot::addEligiblePlayer);
            return;
        }

        // If the eligible players strictly match the current active pot, add to it.
        if (currentEligible.equals(eligiblePlayers)) {
            currentPot.addAmount(amount);
        } else {
            // Eligibility changed -> New Side Pot
            Pot sidePot = new Pot(amount);
            eligiblePlayers.forEach(sidePot::addEligiblePlayer);
            game.getPots().add(sidePot);
        }
    }

    /**
     * Checks if all active players are all-in
     */
    private boolean areAllActivePlayersAllIn(Game game) {
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(Player::isActive)
                .filter(p -> !p.isHasFolded())
                .collect(Collectors.toList());

        // If there's only one active player, they are not considered all-in for this
        // check
        if (activePlayers.size() <= 1) {
            return false;
        }

        // Count how many active players are all-in
        long allInCount = activePlayers.stream()
                .filter(Player::isAllIn)
                .count();

        // If all active players except one are all-in, we consider it an "all players
        // all-in" situation
        // This is because the last player has no one to bet against
        return allInCount >= activePlayers.size() - 1;
    }
}
