
package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.model.GameUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class BettingManager {

    @Autowired
    private GameNotificationService notificationService;

    public void startNewBettingRound(Game game) {
        game.setupNextRound();

        switch (game.getStatus()) {
            case STARTING:
                setupPreFlopBetting(game);
                break;
            case PRE_FLOP_BETTING:
                game.setStatus(Game.GameStatus.FLOP_BETTING);
                break;
            case FLOP_BETTING:
                game.setStatus(Game.GameStatus.TURN_BETTING);
                break;
            case TURN_BETTING:
                game.setStatus(Game.GameStatus.RIVER_BETTING);
                break;
            case RIVER_BETTING:
                game.setStatus(Game.GameStatus.SHOWDOWN);
                break;
        }
        notifyGameUpdate(game.getId(), GameUpdate.GameUpdateType.ROUND_STARTED , game);
    }

    private void setupPreFlopBetting(Game game) {
        // Post small blind
        Player smallBlind = findPlayerById(game, game.getSmallBlindId());
        placeBet(game, smallBlind, game.getSmallBlindAmount());

        // Post big blind
        Player bigBlind = findPlayerById(game, game.getBigBlindId());
        placeBet(game, bigBlind, game.getBigBlindAmount());

        game.setCurrentBet(game.getBigBlindAmount());
        game.setStatus(Game.GameStatus.PRE_FLOP_BETTING);

        // Set first player to act (after big blind)
        int bigBlindPosition = game.getPlayers().indexOf(bigBlind);
        game.setCurrentPlayerIndex((bigBlindPosition + 1) % game.getPlayers().size());
    }

    public void placeBet(Game game, Player player, double amount) {
        if (amount > player.getChips()) {
            amount = player.getChips(); // All-in
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.ALL_IN);
        } else if (amount == 0 && game.getCurrentBet() == 0) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.CHECK);
        } else if (amount == game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.CALL);
        } else if (amount > game.getCurrentBet()) {
            game.getLastActions().put(player.getUsername(), Game.PlayerAction.RAISE);
        }

        player.placeBet(amount);
        game.setPot(game.getPot() + amount);
        game.getCurrentBettingRound().getPlayerBets().put(player.getUsername(), amount);

        if (amount > game.getCurrentBet()) {
            game.setCurrentBet(amount);
        }

        notifyBettingAction(game, player, amount);
    }

    public void fold(Game game, Player player) {
        player.setActive(false);
        game.getLastActions().put(player.getId(), Game.PlayerAction.FOLD);
        notifyPlayerFolded(game, player);

        // Check if only one player remains
        if (getActivePlayerCount(game) == 1) {
            endBettingRound(game);
        }
    }

    public boolean isBettingRoundComplete(Game game) {
        int activePlayerCount = getActivePlayerCount(game);
        if (activePlayerCount <= 1) return true;

        // Check if all active players have acted and bet the same amount
        double targetBet = game.getCurrentBet();
        for (Player player : game.getPlayers()) {
            if (!player.isActive()) continue;

            Double playerBet = game.getCurrentBettingRound().getPlayerBets().getOrDefault(player.getUsername() , 0.0);
            Game.PlayerAction lastAction = game.getLastActions().get(player.getId());

            if (lastAction == Game.PlayerAction.NONE ||
                (playerBet < targetBet && lastAction != Game.PlayerAction.ALL_IN)) {
                return false;
            }
        }

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

    private void notifyGameUpdate(String gameId , GameUpdate.GameUpdateType updateType , Object payload ) {
        GameUpdate update = new GameUpdate();
        update.setGameId(gameId);
        update.setType(updateType);
        update.setPayload(payload);
        notificationService.notifyGameUpdate(update);
    }

    private void notifyBettingAction(Game game, Player player, double amount) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.PLAYER_BET);
        update.setPayload(Map.of(
            "username", player.getUsername(),
            "amount", amount,
            "action", game.getLastActions().get(player.getUsername())
        ));
        notificationService.notifyGameUpdate(update);
    }


    private void notifyPlayerFolded(Game game, Player player) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.PLAYER_FOLDED);
        update.setPayload(Map.of("playerId", player.getUsername()));
        notificationService.notifyGameUpdate(update);
    }

    private void notifyBettingRoundComplete(Game game) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.ROUND_COMPLETE);
        update.setPayload(Map.of(
            "pot", game.getPot(),
            "status", game.getStatus()
        ));
        notificationService.notifyGameUpdate(update);
    }
}
