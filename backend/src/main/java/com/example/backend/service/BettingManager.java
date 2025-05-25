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
    private final List<Game.PlayerAction> notAllowedStatus = List.of(Game.PlayerAction.NONE, Game.PlayerAction.SMALL_BLIND, Game.PlayerAction.BIG_BLIND);

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
        gameScheduler.schedulePlayerTimeout(game.getId() , currentPlayerId);
        
        // Publish event for round started
        eventPublisher.publishEvent(new RoundStartedEvent(game.getId(), game, roundType));
        
        logger.debug("Betting round started. Updated game state: {}", game);
    }

    private void setupPreFlopBetting(Game game) {
        // Post small blind
        Player smallBlind = findPlayerByUsername(game, game.getSmallBlindUserId());
        placeBet(game, smallBlind, game.getSmallBlindAmount() , Game.PlayerAction.SMALL_BLIND);

        // Post big blind
        Player bigBlind = findPlayerByUsername(game, game.getBigBlindUserId());
        placeBet(game, bigBlind, game.getBigBlindAmount() , Game.PlayerAction.BIG_BLIND);

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
            new ArrayList<>(communityCards)
        ));
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
            new ArrayList<>(communityCards)
        ));
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
            new ArrayList<>(communityCards)
        ));
    }

    /**
     * Skips betting rounds and deals all remaining community cards up to the river
     */
    private void skipToRiver(Game game) {
        logger.info("All active players are all-in. Skipping to river for game with ID: {}", game.getId());

        while(game.getStatus() != Game.GameStatus.RIVER_BETTING){
            startNewBettingRound(game);
        }
        // Proceed directly to showdown
        evaluateHandAndAwardPot(game);
        game.setStatus(Game.GameStatus.WAITING);
    }

    public void placeBet(Game game, Player player, double amount, Game.PlayerAction action) {
        logger.info("Player '{}' is placing a bet of {} in game with ID: {}", player.getUsername(), amount, game.getId());
        double betPlacedAmount = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername(), 0.0);
        double betPlacedTotal = betPlacedAmount + amount;
        boolean isAllIn = false;

        // Check for Blinds or All-in
        if (action == Game.PlayerAction.BIG_BLIND || action == Game.PlayerAction.SMALL_BLIND) {
            game.getLastActions().put(player.getUsername(), action);
        }else if (amount == player.getChips()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.ALL_IN);
            isAllIn = true;
        }else if (betPlacedTotal == game.getCurrentBet()) {
            if (amount == 0)
                game.getLastActions().put(player.getUsername(), Game.PlayerAction.CHECK);
            else
                game.getLastActions().put(player.getUsername(), Game.PlayerAction.CALL);
        } else if (betPlacedTotal > game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.RAISE);
        }

        game.setCurrentBet(max(betPlacedTotal,game.getCurrentBet()));

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
    private void handleAllInBet(Game game, Player allInPlayer, double amount) {
        logger.info("Player '{}' is all-in with {} chips", allInPlayer.getUsername(), amount);

        // Get all players who are still active
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(Player::isActive)
                .filter(p -> !p.isHasFolded())
                .toList();

        // Get the total bet of the all-in player
        double allInPlayerTotalBet = game.getCurrentBettingRound().getPlayerBets()
                .getOrDefault(allInPlayer.getUsername(), 0.0);

        // Find players who have bet more than the all-in player
        List<Player> playersWithHigherBets = activePlayers.stream()
                .filter(p -> !p.getId().equals(allInPlayer.getId()))
                .filter(p -> {
                    double playerBet = game.getCurrentBettingRound().getPlayerBets()
                            .getOrDefault(p.getUsername(), 0.0);
                    return playerBet > allInPlayerTotalBet ||
                            (playerBet == allInPlayerTotalBet && p.getChips() > 0); // Players who can still bet more
                })
                .toList();

        // If there are players with higher bets or who can bet more, create a side pot
        if (!playersWithHigherBets.isEmpty()) {
            // Add the all-in amount to the main pot
            game.addToPot(amount);
            
            // Make sure the all-in player is eligible for the main pot
            if (!game.getMainPot().getEligiblePlayerIds().contains(allInPlayer.getId())) {
                game.getMainPot().addEligiblePlayer(allInPlayer.getId());
            }

            // Create a side pot for the remaining players
            // The side pot starts with 0 because future bets will go into it
            Pot sidePot = new Pot(0.0);
            playersWithHigherBets.forEach(p -> sidePot.addEligiblePlayer(p.getId()));
            game.getPots().add(sidePot);

            logger.debug("Created side pot for players: {}",
                    playersWithHigherBets.stream().map(Player::getUsername).collect(Collectors.joining(", ")));

            // If there are multiple all-in players with different amounts, we might need multiple side pots
            List<Player> allInPlayers = activePlayers.stream()
                    .filter(Player::isAllIn)
                    .sorted((p1, p2) -> {
                        // Sort by total bet amount (ascending)
                        double bet1 = game.getCurrentBettingRound().getPlayerBets()
                                .getOrDefault(p1.getUsername(), 0.0);
                        double bet2 = game.getCurrentBettingRound().getPlayerBets()
                                .getOrDefault(p2.getUsername(), 0.0);
                        return Double.compare(bet1, bet2);
                    })
                    .toList();

            if (allInPlayers.size() > 1) {
                logger.debug("Multiple all-in players detected, creating additional side pots if needed");

                // Process each all-in player to create appropriate side pots
                for (int i = 0; i < allInPlayers.size() - 1; i++) {
                    Player currentAllIn = allInPlayers.get(i);
                    Player nextAllIn = allInPlayers.get(i + 1);

                    double currentBet = game.getCurrentBettingRound().getPlayerBets()
                            .getOrDefault(currentAllIn.getUsername(), 0.0);
                    double nextBet = game.getCurrentBettingRound().getPlayerBets()
                            .getOrDefault(nextAllIn.getUsername(), 0.0);

                    if (nextBet > currentBet) {
                        // Create an intermediate side pot that includes the next all-in player
                        // but excludes the current all-in player
                        List<Player> eligibleForNextPot = activePlayers.stream()
                                .filter(p -> !p.getId().equals(currentAllIn.getId()))
                                .filter(p -> {
                                    double playerBet = game.getCurrentBettingRound().getPlayerBets()
                                            .getOrDefault(p.getUsername(), 0.0);
                                    return playerBet >= nextBet ||
                                            (playerBet == nextBet && p.getChips() > 0);
                                })
                                .toList();

                        if (!eligibleForNextPot.isEmpty()) {
                            Pot intermediatePot = new Pot(0.0);
                            eligibleForNextPot.forEach(p -> intermediatePot.addEligiblePlayer(p.getId()));
                            game.getPots().add(intermediatePot);

                            logger.debug("Created intermediate side pot for players: {}",
                                    eligibleForNextPot.stream().map(Player::getUsername).collect(Collectors.joining(", ")));
                        }
                    }
                }
            }
        } else {
            // If no players have bet more or can bet more, just add to the main pot
            game.addToPot(amount);
            
            // Make sure the all-in player is eligible for the main pot
            if (!game.getMainPot().getEligiblePlayerIds().contains(allInPlayer.getId())) {
                game.getMainPot().addEligiblePlayer(allInPlayer.getId());
            }
        }
    }

    public void handleCurrentBettingRound(Game game , String playerId) {
        // Check if betting round is complete
        if (isBettingRoundComplete(game)) {
            gameScheduler.cancelPlayerTimeout(game.getId() , playerId);

            // Update pot amounts before moving to the next round
            updatePotAmounts(game);
            
            if (getActivePlayerCount(game) <= 1 || game.getStatus() == Game.GameStatus.RIVER_BETTING) {
                // Game will be ended by the service that calls this method
                evaluateHandAndAwardPot(game);
                // Instead of immediately setting status to WAITING, the game will be scheduled for restart
                // after a delay in evaluateHandAndAwardPot
            } else if (areAllActivePlayersAllIn(game)) {
                // If all active players are all-in, skip to river or showdown
                skipToRiver(game);
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
            if (!player.isActive() || player.isHasFolded()) continue;

            Double playerBet = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername() , 0.0);
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
                    game.getCommunityCards()
                );
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
                        game.getCommunityCards()
                );
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
    private void updatePotAmounts(Game game) {
        logger.info("Updating pot amounts for game with ID: {}", game.getId());
        
        // Get all players who are still active in the hand
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(p -> p.isActive() || (p.isAllIn() && !p.isHasFolded()))
                .toList();
        
        // If there's only one active player, no need to update pots
        if (activePlayers.size() <= 1) {
            return;
        }
        
        // Reset pot structure - we'll rebuild it correctly
        double totalPotAmount = game.getPot();
        game.setPots(new ArrayList<>());
        game.getPots().add(new Pot(0)); // Start with an empty main pot
        
        // Add all active players to the main pot's eligible players
        activePlayers.forEach(p -> game.getMainPot().addEligiblePlayer(p.getId()));
        
        // Get all players sorted by their bet amount (lowest to highest)
        Map<String, Double> playerBets = new HashMap<>(game.getCurrentBettingRound().getPlayerBets());
        List<Player> sortedPlayers = activePlayers.stream()
                .sorted((p1, p2) -> {
                    double bet1 = playerBets.getOrDefault(p1.getUsername(), 0.0);
                    double bet2 = playerBets.getOrDefault(p2.getUsername(), 0.0);
                    return Double.compare(bet1, bet2);
                })
                .toList();
        
        // If all players bet the same amount, just put everything in the main pot
        if (sortedPlayers.stream().map(p -> playerBets.getOrDefault(p.getUsername(), 0.0)).distinct().count() == 1) {
            game.getMainPot().addAmount(totalPotAmount);
            logger.debug("All players bet the same amount, all in main pot: {}", totalPotAmount);
            return;
        }
        
        // Process players from lowest bet to highest, creating side pots as needed
        double previousBetLevel = 0;
        
        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player currentPlayer = sortedPlayers.get(i);
            double currentBet = playerBets.getOrDefault(currentPlayer.getUsername(), 0.0);
            
            // Skip players who didn't bet
            if (currentBet <= 0) {
                continue;
            }
            
            // If this bet is higher than the previous level, create a pot level
            if (currentBet > previousBetLevel) {
                // Calculate contribution to this pot level from each player
                double levelContribution = currentBet - previousBetLevel;
                double potLevelAmount = 0;
                
                // Calculate eligible players for this pot level
                List<Player> eligiblePlayers = new ArrayList<>();
                
                for (Player player : sortedPlayers) {
                    double playerBet = playerBets.getOrDefault(player.getUsername(), 0.0);
                    
                    if (playerBet >= currentBet) {
                        eligiblePlayers.add(player);
                        potLevelAmount += levelContribution;
                    } else if (playerBet > previousBetLevel) {
                        // Player contributed partially to this level
                        potLevelAmount += (playerBet - previousBetLevel);
                    }
                }
                
                // Determine which pot to add this amount to
                if (i == 0) {
                    // First level goes to main pot
                    game.getMainPot().addAmount(potLevelAmount);
                    logger.debug("Added {} to main pot at bet level {}", potLevelAmount, currentBet);
                } else {
                    // Create a side pot for this level
                    Pot sidePot = new Pot(potLevelAmount);
                    eligiblePlayers.forEach(p -> sidePot.addEligiblePlayer(p.getId()));
                    game.getPots().add(sidePot);
                    logger.debug("Created side pot of {} for {} players at bet level {}", 
                            potLevelAmount, eligiblePlayers.size(), currentBet);
                }
                
                previousBetLevel = currentBet;
            }
        }
        
        // Verify total pot amount is preserved
        double sumOfPots = game.getPots().stream().mapToDouble(Pot::getAmount).sum();
        if (Math.abs(sumOfPots - totalPotAmount) > 0.01) {
            logger.warn("Pot amount discrepancy detected. Original: {}, Sum of pots: {}", totalPotAmount, sumOfPots);
            // Adjust main pot to ensure total is correct
            double adjustment = totalPotAmount - sumOfPots;
            game.getMainPot().addAmount(adjustment);
        }
        
        logger.debug("Updated pot distribution. Total pot: {}, Number of pots: {}", 
                game.getPot(), game.getPots().size());
    }
    
    /**
     * Checks if all active players are all-in
     */
    private boolean areAllActivePlayersAllIn(Game game) {
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(Player::isActive)
                .filter(p -> !p.isHasFolded())
                .collect(Collectors.toList());
        
        // If there's only one active player, they are not considered all-in for this check
        if (activePlayers.size() <= 1) {
            return false;
        }
        
        // Count how many active players are all-in
        long allInCount = activePlayers.stream()
                .filter(Player::isAllIn)
                .count();
        
        // If all active players except one are all-in, we consider it an "all players all-in" situation
        // This is because the last player has no one to bet against
        return allInCount >= activePlayers.size() - 1;
    }
}
