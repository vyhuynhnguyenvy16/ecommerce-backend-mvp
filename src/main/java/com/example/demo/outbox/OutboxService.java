package com.example.demo.outbox;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Transactional outbox: the event row commits in the same transaction as the state change, so an event is never
 * published for a change that rolled back, and never lost for one that committed.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent enqueue(String aggregateType, String aggregateId, String eventType, Object payload) {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, eventType, aggregateType, aggregateId, payload);
        return repository.save(OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonMapper.writeValueAsString(envelope))
                .build());
    }

    /** What goes on the wire: metadata plus the event-specific data. */
    public record EventEnvelope(UUID eventId, String eventType, String aggregateType, String aggregateId,
            Object data) {
    }
}
