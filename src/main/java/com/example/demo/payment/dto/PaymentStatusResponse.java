package com.example.demo.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        AttemptStatus lastAttemptStatus,
        Integer lastResultCode,
        boolean canRetry) {
}
