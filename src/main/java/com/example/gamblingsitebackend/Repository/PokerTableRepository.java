package com.example.gamblingsitebackend.Repository;
import com.example.gamblingsitebackend.Entity.PokerTable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PokerTableRepository extends MongoRepository<PokerTable, String> {
    Optional<PokerTable> findByTableId(String tableId);

}