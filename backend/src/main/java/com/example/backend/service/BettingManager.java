package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.event.CardsDealtEvent;
import com.example.backend.event.GameEndedEvent;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.event.RoundStartedEvent;
import com.example.backend.model.BettingRound;
import com.example.backend.model.Card;
import com.example.backend.model.Player;
import com.example.backend.model.Pot;
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
    private final List<Game.PlayerAction> notAllowedStatus = List.of(Game.PlayerAction.NONE, Game.PlayerAction.SMALL_BLIND, Game.PlayerAction.BIG_BLIND);

    public void startNewBettingRound(Game game) {
        logger.info("Starting a new betting round for game with ID: {}", game.getId());
        game.setupNextRound();

        BettingRound.RoundType roundType = BettingRound.RoundType.PRE_FLOP;

        switch (game.getStatus()) {
            case STARTING:
                setupPreFlopBetting(game);
                roundType = BettingRound.RoundType.PRE_FLOP;
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
            // Calculate how much of the bet goes to the main pot (that all-in player is eligible for)
            game.addToPot(amount);

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
        }
    }

    public void handleCurrentBettingRound(Game game) {
        // Check if betting round is complete
        if (isBettingRoundComplete(game)) {
            // Update pot amounts before moving to the next round
            updatePotAmounts(game);
            
            if (getActivePlayerCount(game) <= 1 || game.getStatus() == Game.GameStatus.RIVER_BETTING) {
                // Game will be ended by the service that calls this method
                evaluateHandAndAwardPot(game);
                game.setStatus(Game.GameStatus.WAITING);
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
                .filter(p -> p.isActive() && !p.isHasFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            logger.warn("No active players, unusual case");
            return;
        }

        if (activePlayers.size() == 1) {
            // Single player gets all pots
            Player winner = activePlayers.get(0);
            winner.awardPot(game.getPot());

            // Publish game ended event
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    List.of(winner),
                    null
            ));

            logger.debug("Single player awarded pot: {}", winner);
            return;
        }

        try {
            // Evaluate each hand
            Map<Player, HandEvaluator.HandResult> playerResults = new HashMap<>();
            for (Player player : activePlayers) {
                HandEvaluator.HandResult result = handEvaluator.evaluateHand(
                        player.getHand(),
                        game.getCommunityCards()
                );
                playerResults.put(player, result);
            }

            // Process each pot separately
            List<Pot> allPots = new ArrayList<>(game.getPots());
            List<Player> allWinners = new ArrayList<>();
            
            for (Pot pot : allPots) {
                // Find eligible players for this pot
                List<Player> eligiblePlayers = activePlayers.stream()
                        .filter(p -> pot.isPlayerEligible(p.getId()) || pot.getEligiblePlayerIds().isEmpty())
                        .toList();
                
                if (eligiblePlayers.isEmpty()) {
                    logger.warn("No eligible players for pot, unusual case");
                    continue;
                }
                
                // Find the best hand among eligible players
                HandEvaluator.HandResult bestResult = null;
                for (Player player : eligiblePlayers) {
                    HandEvaluator.HandResult result = playerResults.get(player);
                    if (bestResult == null || compareHandResults(result, bestResult) > 0) {
                        bestResult = result;
                    }
                }
                
                // Find all winners for this pot
                List<Player> potWinners = new ArrayList<>();
                for (Player player : eligiblePlayers) {
                    HandEvaluator.HandResult result = playerResults.get(player);
                    if (compareHandResults(result, bestResult) == 0) {
                        potWinners.add(player);
                    }
                }
                
                // Split pot among winners
                double winAmount = pot.getAmount() / potWinners.size();
                for (Player winner : potWinners) {
                    winner.awardPot(winAmount);
                    if (!allWinners.contains(winner)) {
                        allWinners.add(winner);
                    }
                }
                
                logger.debug("Pot {} split among winners: {}", 
                        allPots.indexOf(pot), 
                        potWinners.stream().map(Player::getUsername).collect(Collectors.joining(", ")));
            }

            // Publish game ended event with all winners
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    allWinners,
                    null // We don't have a single best result anymore
            ));

        } catch (Exception e) {
            // If there's any error in hand evaluation, split the pot equally among all active players
            double splitAmount = game.getPot() / activePlayers.size();
            for (Player player : activePlayers) {
                player.awardPot(splitAmount);
            }

            // Publish game ended event with error information
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    new ArrayList<>(activePlayers),
                    null
            ));

            logger.error("Error during hand evaluation: {}", e.getMessage());
        }
    }

    // Compare two hand results
    private int compareHandResults(HandEvaluator.HandResult result1, HandEvaluator.HandResult result2) {
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
                .filter(p -> p.isActive() && !p.isHasFolded())
                .toList();
        
        // If there's only one active player, no need to update pots
        if (activePlayers.size() <= 1) {
            return;
        }
        
        // Get all-in players sorted by their bet amount (lowest to highest)
        List<Player> allInPlayers = activePlayers.stream()
                .filter(Player::isAllIn)
                .sorted((p1, p2) -> {
                    double bet1 = game.getCurrentBettingRound().getPlayerBets()
                            .getOrDefault(p1.getUsername(), 0.0);
                    double bet2 = game.getCurrentBettingRound().getPlayerBets()
                            .getOrDefault(p2.getUsername(), 0.0);
                    return Double.compare(bet1, bet2);
                })
                .toList();
        
        // If there are no all-in players, all bets go to the main pot
        if (allInPlayers.isEmpty()) {
            logger.debug("No all-in players, all bets go to the main pot");
            return;
        }
        
        // Process each all-in player to ensure side pots are correctly allocated
        double previousBetLevel = 0;
        for (Player allInPlayer : allInPlayers) {
            double allInBet = game.getCurrentBettingRound().getPlayerBets()
                    .getOrDefault(allInPlayer.getUsername(), 0.0);
            
            // If this all-in bet is higher than the previous level, create a new side pot
            if (allInBet > previousBetLevel) {
                // Players eligible for this pot level (bet at least as much as the all-in player)
                List<Player> eligiblePlayers = activePlayers.stream()
                        .filter(p -> {
                            double playerBet = game.getCurrentBettingRound().getPlayerBets()
                                    .getOrDefault(p.getUsername(), 0.0);
                            return playerBet >= allInBet;
                        })
                        .toList();
                
                // Calculate the amount that goes into this pot level
                double potLevelAmount = (allInBet - previousBetLevel) * eligiblePlayers.size();
                
                // Find or create the appropriate pot for this level
                final Pot[] targetPotRef = {null};
                
                // Check if we already have a pot for this player
                for (Pot pot : game.getPots()) {
                    if (pot.getEligiblePlayerIds().contains(allInPlayer.getId()) && 
                        pot.getEligiblePlayerIds().size() == eligiblePlayers.size()) {
                        // This pot might be the right one, check if all eligible players are included
                        boolean allEligibleIncluded = true;
                        for (Player p : eligiblePlayers) {
                            if (!pot.getEligiblePlayerIds().contains(p.getId())) {
                                allEligibleIncluded = false;
                                break;
                            }
                        }
                        
                        if (allEligibleIncluded) {
                            targetPotRef[0] = pot;
                            break;
                        }
                    }
                }
                
                // If no existing pot was found, create a new one
                if (targetPotRef[0] == null) {
                    Pot newPot = new Pot(potLevelAmount);
                    eligiblePlayers.forEach(p -> newPot.addEligiblePlayer(p.getId()));
                    game.getPots().add(newPot);
                    logger.debug("Created new pot level of {} for {} players at bet level {}", 
                            potLevelAmount, eligiblePlayers.size(), allInBet);
                } else {
                    // Add to existing pot
                    targetPotRef[0].addAmount(potLevelAmount);
                    logger.debug("Added {} to existing pot for bet level {}", 
                            potLevelAmount, allInBet);
                }
                
                previousBetLevel = allInBet;
            }
        }
        
        // Any remaining bets above the highest all-in go to the main pot or a final side pot
        double highestAllInBet = previousBetLevel;
        List<Player> playersAboveHighestAllIn = activePlayers.stream()
                .filter(p -> {
                    double playerBet = game.getCurrentBettingRound().getPlayerBets()
                            .getOrDefault(p.getUsername(), 0.0);
                    return playerBet > highestAllInBet;
                })
                .toList();
        
        if (!playersAboveHighestAllIn.isEmpty()) {
            // Calculate the excess amount above the highest all-in
            double excessAmount = 0;
            for (Player p : playersAboveHighestAllIn) {
                double playerBet = game.getCurrentBettingRound().getPlayerBets()
                        .getOrDefault(p.getUsername(), 0.0);
                excessAmount += (playerBet - highestAllInBet);
            }
            
            // Create a final side pot for players who bet above the highest all-in
            if (excessAmount > 0) {
                Pot finalPot = new Pot(excessAmount);
                playersAboveHighestAllIn.forEach(p -> finalPot.addEligiblePlayer(p.getId()));
                game.getPots().add(finalPot);
                logger.debug("Created final side pot of {} for {} players who bet above highest all-in", 
                        excessAmount, playersAboveHighestAllIn.size());
            }
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
