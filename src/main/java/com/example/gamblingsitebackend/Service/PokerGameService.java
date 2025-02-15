package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.PokerTable;
import com.example.gamblingsitebackend.Extras.GameUpdate;
import com.example.gamblingsitebackend.Extras.Player;
import com.example.gamblingsitebackend.Repository.PokerTableRepository;
import com.example.gamblingsitebackend.Logics.PokerWinnerLogic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PokerGameService {

    private final PokerTableRepository pokerTableRepository;
    private final Map<String, List<String>> playerHands = new HashMap<>();
    private final Map<String, Integer> playerBets = new HashMap<>();
    private List<String> communityCards = new ArrayList<>();
    private List<String> deck = new ArrayList<>();
    private int pot = 0;
    private int currentBetAmount = 0;

    private List<String> createDeck() {
        String[] suits = {"♠", "♥", "♣", "♦"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        List<String> deck = new ArrayList<>();
        for (String rank : ranks) {
            for (String suit : suits) {
                deck.add(rank + suit);
            }
        }
        return deck;
    }

    public GameUpdate startNewGame(String tableId) {
        this.deck = createDeck();
        Collections.shuffle(deck);
        this.communityCards = new ArrayList<>();
        this.playerHands.clear();
        this.playerBets.clear();
        this.pot = 0;
        this.currentBetAmount = 0;

        GameUpdate gameUpdate = new GameUpdate();
        PokerTable table = pokerTableRepository.findByTableId(tableId).stream().findFirst().orElse(null);

        if (table != null) {
            for (Player player : table.getPlayers()) {
                playerHands.put(player.getUsername(), deck.subList(0, 2));
                deck = deck.subList(2, deck.size());
            }
            gameUpdate.setType("GAME_START");
            gameUpdate.setPlayers(table.getPlayers());
        }

        return gameUpdate;
    }
    public GameUpdate placeCheck(String tableId, String username) {
        boolean isValid = playerBets.getOrDefault(username, 0) >= currentBetAmount;
        GameUpdate gameUpdate = new GameUpdate();
        gameUpdate.setType("PLAYER_ACTION");
        if (isValid) {
            gameUpdate.setMessage("Player " + username + " check.");
        }else{
            gameUpdate.setMessage("Player " + username + " cannot check at this time.");
        }
        return gameUpdate;
    }

    public GameUpdate placeBet(String tableId, String username, int betAmount) {
        playerBets.put(username, playerBets.getOrDefault(username, 0) + betAmount);
        pot += betAmount;
        GameUpdate gameUpdate = new GameUpdate();
        gameUpdate.setType("POT_UPDATE");
        gameUpdate.setPot((double) pot);

        return gameUpdate;
    }

    public GameUpdate placeRaise(String tableId, String username, int raiseAmount) {
        return placeBet(tableId, username, raiseAmount);
    }

    public GameUpdate foldPlayer(String username) {
        playerHands.remove(username);
        GameUpdate gameUpdate = new GameUpdate();
        gameUpdate.setType("PLAYER_ACTION");
        gameUpdate.setMessage("Player " + username + " Folded.");
        return gameUpdate;
    }

    public GameUpdate dealCommunityCards(String tableId) {
        GameUpdate gameUpdate = new GameUpdate();
        if (!deck.isEmpty()) {
            List<String> cardsToAdd = deck.subList(0, Math.min(3, deck.size()));
            communityCards.addAll(cardsToAdd);
            deck = deck.subList(cardsToAdd.size(), deck.size());

            gameUpdate.setType("COMMUNITY_CARDS");
            gameUpdate.setCommunityCards(communityCards);
        }
        return gameUpdate;
    }

    public List<String> dealHandCards(String username) {
        return playerHands.get(username);
    }

    public GameUpdate getPlayersForWinnerEvaluation(String tableId) {
        GameUpdate gameUpdate = new GameUpdate();
        PokerTable table = pokerTableRepository.findByTableId(tableId).stream().findFirst().orElse(null);
        List<PokerWinnerLogic.Player> winners = new ArrayList<>();
        if (table != null) {
            for (Player player : table.getPlayers()) {
                winners.add(new PokerWinnerLogic.Player(player.getUsername(), playerHands.getOrDefault(player.getUsername(), new ArrayList<>())));
            }
        }
        PokerWinnerLogic.Player winner = PokerWinnerLogic.determineWinner(winners);
        return gameUpdate;
    }
}