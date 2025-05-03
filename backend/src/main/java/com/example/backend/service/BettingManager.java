package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Card;
import com.example.backend.model.Player;
import com.example.backend.model.GameUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BettingManager {
    private static final Logger logger = LoggerFactory.getLogger(BettingManager.class);

    @Autowired
    private GameNotificationService notificationService;

    public void startNewBettingRound(Game game) {
        logger.info("Starting a new betting round for game with ID: {}", game.getId());
        game.setupNextRound();

        switch (game.getStatus()) {
            case STARTING:
                setupPreFlopBetting(game);
                break;
            case PRE_FLOP_BETTING:
                setupFlopBetting(game);
                break;
            case FLOP_BETTING:
                setupTurnBetting(game);
                break;
            case TURN_BETTING:
                setupRiverBetting(game);
                break;
            case RIVER_BETTING:
                game.setStatus(Game.GameStatus.SHOWDOWN);
                break;
        }

        notifyRoundStart(game.getId(), new Game(game));
        logger.debug("Betting round started. Updated game state: {}", game);
    }

    private void setupPreFlopBetting(Game game) {
        // Post small blind
        Player smallBlind = findPlayerByUsername(game, game.getSmallBlindUserId());
        placeBet(game, smallBlind, game.getSmallBlindAmount());

        // Post big blind
        Player bigBlind = findPlayerByUsername(game, game.getBigBlindUserId());
        placeBet(game, bigBlind, game.getBigBlindAmount());

        game.setCurrentBet(game.getBigBlindAmount());
        game.setStatus(Game.GameStatus.PRE_FLOP_BETTING);

        // Set first player to act (after big blind)
        int bigBlindPosition = game.getPlayers().indexOf(bigBlind);
        game.setCurrentPlayerIndex((bigBlindPosition + 1) % game.getPlayers().size());
    }

    private void setupFlopBetting(Game game) {
        game.setCurrentBet(0);
        game.setStatus(Game.GameStatus.FLOP_BETTING);

        List<Card> communityCards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            communityCards.add(game.getDeck().drawCard());
        }
        game.setCommunityCards(communityCards);
    }

    private void setupTurnBetting(Game game) {
        game.setCurrentBet(0);
        game.setStatus(Game.GameStatus.TURN_BETTING);

        // Add one card to the community cards
        List<Card> communityCards = game.getCommunityCards();
        communityCards.add(game.getDeck().drawCard());
        game.setCommunityCards(communityCards);
    }

    private void setupRiverBetting(Game game) {
        game.setCurrentBet(0);
        game.setStatus(Game.GameStatus.RIVER_BETTING);

        // Add one card to the community cards
        List<Card> communityCards = game.getCommunityCards();
        communityCards.add(game.getDeck().drawCard());
        game.setCommunityCards(communityCards);
    }

    public void placeBet(Game game, Player player, double amount) {
        logger.info("Player '{}' is placing a bet of {} in game with ID: {}", player.getUsername(), amount, game.getId());
        double betPlacedAmount = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername() , 0.0);
        double betPlacedTotal = betPlacedAmount + amount;
        if (amount > player.getChips()) {
            amount = player.getChips(); // All-in
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.ALL_IN);
        } else if (amount == 0) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.CHECK);
        } else if (betPlacedTotal == game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.CALL);
        } else if (betPlacedTotal > game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.RAISE);
            game.setCurrentBet(betPlacedTotal);
        }

        player.placeBet(amount);
        game.setPot(game.getPot() + amount);
        game.getCurrentBettingRound().getPlayerBets().put(player.getUsername(), betPlacedTotal);

//        notifyBettingAction(game, player, amount);
        logger.debug("Bet placed. Updated game state: {}", game);
    }

    public void fold(Game game, Player player) {
        logger.info("Player '{}' is folding in game with ID: {}", player.getUsername(), game.getId());
        player.setActive(false);
        game.getLastActions().put(player.getId(), Game.PlayerAction.FOLD);
        notifyPlayerFolded(game, player);

        // Check if only one player remains
        if (getActivePlayerCount(game) == 1) {
            endBettingRound(game);
        }
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

            if (lastAction == Game.PlayerAction.NONE ||
                (playerBet < targetBet && lastAction != Game.PlayerAction.ALL_IN)) {
                logger.debug("Betting round complete status: false");
                return false;
            }
        }

        logger.debug("Betting round complete status: true");
        return true;
    }

    private void endBettingRound(Game game) {
        game.getCurrentBettingRound().setRoundComplete(true);
        notifyBettingRoundComplete(game);
    }

    private int getActivePlayerCount(Game game) {
        return (int) game.getPlayers().stream()
                .filter(Player::isActive)
                .count();
    }

    private Player findPlayerById(Game game, String playerId) {
        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private Player findPlayerByUsername(Game game, String playerUsername) {
        return game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(playerUsername))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private void notifyRoundStart(String gameId, Object payload) {
        GameUpdate update = new GameUpdate();
        update.setGameId(gameId);
        update.setType(GameUpdate.GameUpdateType.ROUND_STARTED);
        update.setPayload(payload);
        notificationService.notifyGameUpdate(update);
    }

    private void notifyBettingAction(Game game, Player player, double amount) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.PLAYER_BET);
        update.setPayload(Map.of(
            "playerId", player.getId(),
            "username", player.getUsername(),
            "amount", amount,
            "action", game.getLastActions().get(player.getUsername()),
            "pot", game.getPot(),
            "currentBet", game.getCurrentBet()
        ));
        notificationService.notifyGameUpdate(update);
    }

    private void notifyPlayerFolded(Game game, Player player) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.PLAYER_FOLDED);
        update.setPayload(Map.of(
            "playerId", player.getId(),
            "username", player.getUsername(),
            "remainingPlayers", getActivePlayerCount(game)
        ));
        notificationService.notifyGameUpdate(update);
    }

    private void notifyBettingRoundComplete(Game game) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.ROUND_COMPLETE);
        update.setPayload(Map.of(
            "pot", game.getPot(),
            "status", game.getStatus(),
            "communityCards", game.getCommunityCards(),
            "activePlayers", getActivePlayerCount(game)
        ));
        notificationService.notifyGameUpdate(update);
    }
}
