package com.example.demo.payment.dto;

import java.time.LocalDateTime;

/** Where to send the customer to pay, and until when the order waits for payment. */
public record PaymentLinkResponse(Long orderId, String payUrl, LocalDateTime expiresAt) {
}
