package com.example.demo.payment;

import java.time.LocalDateTime;

/** Integration events published (through the outbox) on topic {@code vestra.payments}. */
public final class PaymentEvents {

    public static final String AGGREGATE = "Order";
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_ANOMALY = "PaymentAnomalyDetected";
    public static final String PAYMENT_EXPIRED = "PaymentExpired";

    private PaymentEvents() {
    }

    public record PaymentSucceeded(Long orderId, Long paymentId, long amount, Long momoTransId,
            LocalDateTime paidAt) {
    }

    public record PaymentExpired(Long orderId, Long paymentId, LocalDateTime expiredAt) {
    }

    public record PaymentAnomalyDetected(Long orderId, Long paymentId, String anomaly, Long amount,
            Long momoTransId, String providerOrderId) {
    }
}
