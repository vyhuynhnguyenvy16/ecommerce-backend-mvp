package com.example.demo.outbox;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Placeholder sender; swap for e-mail/push when a provider is chosen. */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void paymentReceived(String orderId, long amount) {
        log.info("Notify customer: payment of {} VND received for order #{}", amount, orderId);
    }
}
