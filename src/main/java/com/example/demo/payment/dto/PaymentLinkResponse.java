package com.example.demo.payment.dto;

import java.time.OffsetDateTime;

/** Where to send the customer to pay, and until when the order waits for payment (with UTC offset). */
public record PaymentLinkResponse(Long orderId, String payUrl, OffsetDateTime expiresAt) {
}
