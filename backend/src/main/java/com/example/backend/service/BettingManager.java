package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.event.CardsDealtEvent;
import com.example.backend.event.GameEndedEvent;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.event.RoundStartedEvent;
import com.example.backend.model.BettingRound;
import com.example.backend.model.Card;
import com.example.backend.model.Player;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

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
            case RIVER_BETTING:
                game.setStatus(Game.GameStatus.SHOWDOWN);
                roundType = BettingRound.RoundType.SHOWDOWN;
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

    public void placeBet(Game game, Player player, double amount , Game.PlayerAction action) {
        logger.info("Player '{}' is placing a bet of {} in game with ID: {}", player.getUsername(), amount, game.getId());
        double betPlacedAmount = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername() , 0.0);

        double betPlacedTotal = betPlacedAmount + amount;

        // Check for Blinds
        if (amount >= player.getChips()) {
            amount = player.getChips(); // All-in
            betPlacedTotal = betPlacedAmount + amount;
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.ALL_IN);
        }else if(action == Game.PlayerAction.BIG_BLIND || action == Game.PlayerAction.SMALL_BLIND) {
            game.getLastActions().put(player.getUsername(), action);
        }else if (betPlacedTotal == game.getCurrentBet()){
            if(amount == 0)
                game.getLastActions().put(player.getUsername(), Game.PlayerAction.CHECK);
            else
                game.getLastActions().put(player.getUsername(), Game.PlayerAction.CALL);
        }else if (betPlacedTotal > game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.RAISE);
        }
        game.setCurrentBet(betPlacedTotal);
        player.placeBet(amount);
        game.setPot(game.getPot() + amount);
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
            if (!player.isActive()) continue;

            Double playerBet = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername() , 0.0);
            Game.PlayerAction lastAction = game.getLastActions().get(player.getUsername());

            if (notAllowedStatus.contains(lastAction) ||
                (playerBet < targetBet && lastAction != Game.PlayerAction.ALL_IN)) {
                logger.debug("Betting round complete status: false");
                return false;
            }
        }

        logger.debug("Betting round complete status: true");
        return true;
    }

    public void evaluateHandAndAwardPot(Game game) {
        logger.info("Evaluating hands and awarding pot for game with ID: {}", game.getId());

        game.setStatus(Game.GameStatus.SHOWDOWN);
        // Only evaluate if there are multiple players still in the hand
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(p -> p.isActive() && !p.isHasFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            logger.warn("No active players, unusual case");
            return;
        }

        if (activePlayers.size() == 1) {
            // Single player gets the pot
            Player winner = activePlayers.get(0);
            winner.awardPot(game.getPot());

            // Publish game ended event
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    List.of(winner)
            ));

            logger.debug("Single player awarded pot: {}", winner);
            return;
        }

        try {
            // Multiple players - evaluate hands and find winner(s)
            Map<Player, HandEvaluator.HandResult> playerResults = new HashMap<>();
            HandEvaluator.HandResult bestResult = null;

            // Evaluate each hand
            for (Player player : activePlayers) {
                HandEvaluator.HandResult result = handEvaluator.evaluateHand(
                        player.getHand(),
                        game.getCommunityCards()
                );

                playerResults.put(player, result);

                // Track the best hand
                if (bestResult == null || compareHandResults(result, bestResult) > 0) {
                    bestResult = result;
                }
            }

            // Find all players with the best hand (could be multiple in case of a tie)
            List<Player> winners = new ArrayList<>();
            for (Map.Entry<Player, HandEvaluator.HandResult> entry : playerResults.entrySet()) {
                if (compareHandResults(entry.getValue(), bestResult) == 0) {
                    winners.add(entry.getKey());
                }
            }

            // Split pot among winners
            double winAmount = game.getPot() / winners.size();
            for (Player winner : winners) {
                winner.awardPot(winAmount);
            }

            // Publish game ended event
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    new ArrayList<>(winners)
            ));

            logger.debug("Pot split among winners: {}", winners);
        } catch (Exception e) {
            // If there's any error in hand evaluation, split the pot equally
            double splitAmount = game.getPot() / activePlayers.size();
            for (Player player : activePlayers) {
                player.awardPot(splitAmount);
            }

            // Publish game ended event with error information
            eventPublisher.publishEvent(new GameEndedEvent(
                    game.getId(),
                    new Game(game),
                    new ArrayList<>(activePlayers)
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
}
