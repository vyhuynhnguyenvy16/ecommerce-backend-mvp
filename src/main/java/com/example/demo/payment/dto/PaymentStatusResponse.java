package com.example.demo.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.demo.payment.AttemptStatus;
import com.example.demo.payment.PaymentMethod;
import com.example.demo.payment.PaymentStatus;

/**
 * Source of truth for the storefront after the MoMo redirect: the UI never trusts redirect query parameters.
 */
public record PaymentStatusResponse(
        Long orderId,
        String orderStatus,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        OffsetDateTime expiresAt,
        OffsetDateTime paidAt,
        AttemptStatus lastAttemptStatus,
        Integer lastResultCode,
        boolean canRetry) {
}
