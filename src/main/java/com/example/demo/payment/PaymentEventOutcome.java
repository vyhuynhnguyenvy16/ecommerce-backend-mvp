package com.example.demo.payment;

/** What VESTRA did with an incoming payment result. Anomalies are picked up by reconciliation. */
public enum PaymentEventOutcome {
    /** Payment moved to PAID. */
    PAID,
    /** Same successful result received again (same transId): ignored. */
    DUPLICATE_IGNORED,
    /** MoMo reports a pending state; nothing changes. */
    STILL_PENDING,
    /** That attempt failed; the customer may retry while the payment window is open. */
    ATTEMPT_FAILED,
    /** Result for a payment already in a final state that does not change it (e.g. a late failure). */
    IGNORED_FINAL,
    /** Signature did not verify: rejected, nothing changes. */
    INVALID_SIGNATURE,
    /** partnerCode is not ours: rejected, nothing changes. */
    WRONG_PARTNER,

    // ---- anomalies: money may have moved but the state is inconsistent; needs a human ----
    /** Reported amount differs from what we asked MoMo to collect. */
    AMOUNT_MISMATCH,
    /** orderId is unknown to VESTRA. */
    UNKNOWN_ORDER,
    /** A second, different successful MoMo transaction for an order that is already PAID. */
    DUPLICATE_PAYMENT,
    /** Money arrived after the order expired or was cancelled. */
    PAID_AFTER_CLOSE;

    public boolean isAnomaly() {
        return this == AMOUNT_MISMATCH || this == UNKNOWN_ORDER || this == DUPLICATE_PAYMENT
                || this == PAID_AFTER_CLOSE;
    }
}
