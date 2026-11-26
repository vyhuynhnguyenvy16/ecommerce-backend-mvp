package com.example.demo.payment;

/** A verified payment result from MoMo, whatever channel it came through (IPN or status query). */
public record PaymentResult(
        PaymentEvent.Source source,
        String providerOrderId,
        Long amount,
        Long transId,
        int resultCode,
        String payload) {
}
