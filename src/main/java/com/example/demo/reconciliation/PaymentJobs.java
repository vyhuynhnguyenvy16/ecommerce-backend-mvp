package com.example.demo.reconciliation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.payment.PaymentExpiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Schedules the payment jobs. Safe on several instances at once: expiry locks each payment row and reconciliation
 * items are unique per (type, reference).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentJobs {

    private final PaymentExpiryService expiryService;
    private final ReconciliationService reconciliationService;

    @Scheduled(fixedDelayString = "${vestra.payments.expiry-check-interval:1m}",
            initialDelayString = "${vestra.payments.expiry-check-interval:1m}")
    public void expirePayments() {
        try {
            expiryService.expireDuePayments();
        } catch (RuntimeException e) {
            log.error("Payment expiry run failed", e);
        }
    }

    @Scheduled(fixedDelayString = "${vestra.payments.reconciliation-interval:15m}",
            initialDelayString = "${vestra.payments.reconciliation-interval:15m}")
    public void reconcile() {
        try {
            reconciliationService.run("SCHEDULED");
        } catch (RuntimeException e) {
            log.error("Reconciliation run failed", e);
        }
    }
}
