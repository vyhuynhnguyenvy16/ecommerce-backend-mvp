package com.example.demo.payment;

/**
 * Order-level payment status (stored on both {@code payments.status} and {@code orders.payment_status}).
 * A failed MoMo attempt does not fail the payment: the customer may retry until the payment expires.
 */
public enum PaymentStatus {
    /** COD orders: nothing to collect online. */
    NOT_REQUIRED,
    PENDING,
    PAID,
    EXPIRED,
    CANCELLED;

    public boolean isFinal() {
        return this == PAID || this == EXPIRED || this == CANCELLED;
    }
}
