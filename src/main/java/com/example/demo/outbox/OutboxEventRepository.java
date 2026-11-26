package com.example.demo.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Claims a batch of unpublished events. SKIP LOCKED lets several app instances run the relay at the same time
     * without sending the same row twice concurrently.
     */
    @Query(value = """
            select * from outbox_events
            where published_at is null
            order by id
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockUnpublishedBatch(@Param("limit") int limit);

    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByIdAsc(String aggregateType, String aggregateId);

    Optional<OutboxEvent> findByEventId(UUID eventId);

    long countByPublishedAtIsNull();
}
