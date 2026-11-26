package com.example.demo.outbox;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.payment.PaymentEvents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Example downstream consumer of {@code vestra.payments}. Idempotent: a redelivered event (same eventId) is skipped,
 * because the processed_events insert and the side effect share one transaction.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "vestra.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    public static final String GROUP = "order-notifications";

    private final ProcessedEventStore processedEvents;
    private final NotificationSender notificationSender;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "${vestra.kafka.topic}", groupId = GROUP)
    @Transactional
    public void onMessage(ConsumerRecord<String, String> record) {
        Header typeHeader = record.headers().lastHeader(KafkaOutboxRelay.EVENT_TYPE_HEADER);
        Header idHeader = record.headers().lastHeader(KafkaOutboxRelay.EVENT_ID_HEADER);
        if (typeHeader == null || idHeader == null) {
            log.warn("Skipping message without event headers at offset {}", record.offset());
            return;
        }
        String eventType = new String(typeHeader.value(), StandardCharsets.UTF_8);
        if (!PaymentEvents.PAYMENT_SUCCEEDED.equals(eventType)) {
            return;
        }
        UUID eventId = UUID.fromString(new String(idHeader.value(), StandardCharsets.UTF_8));
        if (!processedEvents.markProcessed(GROUP, eventId)) {
            log.info("Event {} already processed by {}, skipping", eventId, GROUP);
            return;
        }
        JsonNode data = jsonMapper.readTree(record.value()).path("data");
        notificationSender.paymentReceived(data.path("orderId").asString(), data.path("amount").asLong());
    }
}
