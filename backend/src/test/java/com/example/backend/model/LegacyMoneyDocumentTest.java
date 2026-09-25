package com.example.backend.model;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Games already in Atlas store chips as BSON doubles (1000.0); they must still load now that chips are long. */
class LegacyMoneyDocumentTest {

    private MappingMongoConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, new MongoMappingContext());
        converter.afterPropertiesSet();
    }

    @Test
    void playerWithDecimalChipsLoadsAsWholeChips() {
        Player p = converter.read(Player.class,
                new Document("chips", 1000.0).append("currentBet", 20.0).append("lastWinAmount", 49.5));

        assertEquals(1000, p.getChips());
        assertEquals(20, p.getCurrentBet());
        assertEquals(49, p.getLastWinAmount()); // fractional legacy chips truncate
    }

    @Test
    void potAndRoundBetsWithDecimalAmountsLoad() {
        Pot pot = converter.read(Pot.class, new Document("amount", 150.0));
        BettingRound round = converter.read(BettingRound.class,
                new Document("bets", new Document("p1", 50.0).append("p2", 100.0)));

        assertEquals(150, pot.getAmount());
        assertEquals(50L, round.getBets().get("p1"));
        assertEquals(100L, round.getBets().get("p2"));
    }
}
