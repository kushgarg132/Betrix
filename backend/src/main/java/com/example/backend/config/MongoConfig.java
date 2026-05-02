package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * MongoDB configuration to handle OffsetDateTime serialization/deserialization.
 * MongoDB stores dates as BSON Date (java.util.Date). These converters bridge
 * Date ↔ OffsetDateTime so that the Game entity (which now uses OffsetDateTime
 * for GraphQL DateTime scalar compatibility) can be persisted correctly.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new DateToOffsetDateTimeConverter(),
                new OffsetDateTimeToDateConverter()
        ));
    }

    /**
     * Reads a BSON Date back as OffsetDateTime (UTC).
     */
    private static class DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date source) {
            return source.toInstant().atOffset(ZoneOffset.UTC);
        }
    }

    /**
     * Writes an OffsetDateTime as a BSON Date.
     */
    private static class OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime source) {
            return Date.from(source.toInstant());
        }
    }
}
