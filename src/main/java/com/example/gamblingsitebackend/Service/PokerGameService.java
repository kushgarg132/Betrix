package com.example.gamblingsitebackend.Service;
import com.example.gamblingsitebackend.Entity.PokerTable;
import com.example.gamblingsitebackend.Repository.PokerTableRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PokerGameService {

    private final PokerTableRepository pokerTableRepository;

    public PokerGameService(PokerTableRepository pokerTableRepository) {
        this.pokerTableRepository = pokerTableRepository;
    }

    public PokerTable startNewGame(String tableId, List<String> players) {
        List<String> deck = createDeck();
        Collections.shuffle(deck);

        Map<String, List<String>> playerHands = new HashMap<>();
        for (String player : players) {
            playerHands.put(player, deck.subList(0, 5));  // Deal 5 cards to each player
            deck = deck.subList(5, deck.size());
        }

        PokerTable pokerTable = new PokerTable();
        pokerTable.setTableId(tableId);
        pokerTable.setPlayers(players);
        pokerTable.setPlayerHands(playerHands);
        pokerTable.setCurrentTurn(0);
        pokerTable.setPot(0);

        return pokerTableRepository.save(pokerTable);
    }

    private List<String> createDeck() {
        String[] suits = {"♠", "♥", "♣", "♦"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        List<String> deck = new ArrayList<>();
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + suit);
            }
        }
        return deck;
    }
}
