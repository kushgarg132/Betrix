package com.example.gamblingsitebackend.Service;

import com.example.gamblingsitebackend.Entity.PokerTable;
import com.example.gamblingsitebackend.Extras.Player;
import com.example.gamblingsitebackend.Model.TableResponse;
import com.example.gamblingsitebackend.Repository.PokerTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class PokerTableService {
    private final PokerTableRepository pokerTableRepository;


    public List<PokerTable> getAvailableTables() {
        return pokerTableRepository.findAll();
    }

    public Object joinTable(String tableId, Player player) {

        PokerTable pokerTable = pokerTableRepository.findByTableId(tableId).stream().findFirst().orElse(null);
        if(Objects.nonNull(pokerTable)){
            pokerTable.getPlayers().add(player);
            pokerTableRepository.save(pokerTable);

            return new TableResponse(tableId,"Success" );
        }
        return new TableResponse(tableId,"Failed" );
    }

    public void createTable() {
        PokerTable pokerTable = new PokerTable();
        pokerTable.setTableId(UUID.randomUUID().toString());
        pokerTable.setCurrentTurn(0);
        pokerTable.setBlinds(Pair.of(25.0,50.0));
        pokerTable.setPlayers(List.of());
        pokerTable.setPotLimit(500);
        pokerTableRepository.save(pokerTable);
    }
    public PokerTable getPokerTable(String tableId) {
        return pokerTableRepository.findByTableId(tableId).stream().findFirst().orElse(null);
    }
}
