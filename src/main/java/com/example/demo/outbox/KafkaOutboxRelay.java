package com.example.demo.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes outbox rows to Kafka. Only exists when {@code vestra.kafka.enabled=true}; otherwise rows simply stay
 * unpublished in the database (production on Railway runs without Kafka by default).
 * Delivery is at-least-once: if the app dies after Kafka acknowledged but before the commit, the row is sent again,
 * which is why every message carries an eventId header for consumer de-duplication.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "vestra.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class KafkaOutboxRelay {

    public static final String EVENT_ID_HEADER = "eventId";
    public static final String EVENT_TYPE_HEADER = "eventType";

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    @Value("${vestra.kafka.topic}")
    private String topic;

    @Value("${vestra.kafka.relay-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${vestra.kafka.relay-interval:1s}")
    public void scheduledPublish() {
        publishBatch();
    }

    /** Returns how many events were published. */
    @Transactional
    public int publishBatch() {
        List<OutboxEvent> batch = repository.lockUnpublishedBatch(batchSize);
        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getAggregateId(),
                        event.getPayload());
                record.headers().add(EVENT_ID_HEADER, event.getEventId().toString().getBytes(StandardCharsets.UTF_8));
                record.headers().add(EVENT_TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
                event.setPublishedAt(LocalDateTime.now(clock));
                published++;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                event.setPublishAttempts(event.getPublishAttempts() + 1);
                String message = String.valueOf(e.getMessage());
                event.setLastError(message.length() > 500 ? message.substring(0, 500) : message);
                log.warn("Outbox event {} not published yet: {}", event.getEventId(), message);
                // Keep order per run: stop at the first failure and retry the rest on the next tick.
                break;
            }
        }
        return published;
    }
}
