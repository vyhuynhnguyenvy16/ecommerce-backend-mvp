package com.example.demo.outbox;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/** Consumer-side de-duplication: an event id is handled at most once per consumer group. */
@Component
@RequiredArgsConstructor
public class ProcessedEventStore {

    private final JdbcTemplate jdbc;

    /**
     * Records the event as processed. Returns {@code false} if this group already processed it. Call it in the same
     * transaction as the consumer's side effects so both commit together.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String consumerGroup, UUID eventId) {
        int inserted = jdbc.update("""
                insert into processed_events (consumer_group, event_id) values (?, ?)
                on conflict do nothing
                """, consumerGroup, eventId);
        return inserted == 1;
    }
}
