package com.example.demo.payment;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.ledger.LedgerAccounts;
import com.example.demo.ledger.LedgerService;
import com.example.demo.ledger.LedgerService.Line;
import com.example.demo.outbox.OutboxService;
import com.example.demo.payment.momo.MomoResultCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Applies a verified MoMo result to VESTRA's state. Safe to call any number of times with the same result:
 * the payment row is locked, final states never move backwards, and a PAID payment remembers its transId.
 * IPN and the status-query jobs both go through here, so every channel follows the same rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentResultProcessor {

    public static final String LEDGER_SOURCE = "MOMO_PAYMENT";

    private final PaymentAttemptRepository attemptRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;
    private final Clock clock;

    @Transactional
    public PaymentEventOutcome apply(PaymentResult result) {
        Optional<PaymentAttempt> found = attemptRepository.findByProviderOrderId(result.providerOrderId());
        if (found.isEmpty()) {
            log.warn("MoMo result for unknown orderId {}", result.providerOrderId());
            return record(result, null, PaymentEventOutcome.UNKNOWN_ORDER);
        }
        PaymentAttempt attempt = found.get();
        Payment payment = paymentRepository.findByIdForUpdate(attempt.getPayment().getId()).orElseThrow();

        if (!Objects.equals(result.amount(), attempt.getAmount())) {
            log.error("MoMo amount mismatch for {}: expected {}, got {}", result.providerOrderId(),
                    attempt.getAmount(), result.amount());
            return record(result, payment, PaymentEventOutcome.AMOUNT_MISMATCH);
        }

        int code = result.resultCode();
        if (MomoResultCodes.isSuccess(code)) {
            return onSuccess(result, attempt, payment);
        }
        if (MomoResultCodes.isPending(code)) {
            return record(result, payment, PaymentEventOutcome.STILL_PENDING);
        }
        return onFailure(result, attempt, payment);
    }

    private PaymentEventOutcome onSuccess(PaymentResult result, PaymentAttempt attempt, Payment payment) {
        attempt.setStatus(AttemptStatus.SUCCEEDED);
        attempt.setResultCode(result.resultCode());
        attempt.setTransId(result.transId());

        switch (payment.getStatus()) {
            case PAID -> {
                if (Objects.equals(payment.getProviderTransId(), result.transId())) {
                    return record(result, payment, PaymentEventOutcome.DUPLICATE_IGNORED);
                }
                log.error("Second MoMo payment {} for already-paid payment {}", result.transId(), payment.getId());
                postRefundPayable(result, payment, "paid twice");
                return record(result, payment, PaymentEventOutcome.DUPLICATE_PAYMENT);
            }
            case EXPIRED, CANCELLED -> {
                log.error("MoMo payment {} arrived after payment {} was {}", result.transId(), payment.getId(),
                        payment.getStatus());
                postRefundPayable(result, payment, "paid after " + payment.getStatus().name().toLowerCase());
                return record(result, payment, PaymentEventOutcome.PAID_AFTER_CLOSE);
            }
            default -> {
                markPaid(payment, result.transId());
                long amount = result.amount();
                ledgerService.post(LEDGER_SOURCE, ledgerSourceId(result), "MoMo payment for order #"
                        + payment.getOrder().getId(), List.of(
                                Line.debit(LedgerAccounts.MOMO_RECEIVABLE, amount),
                                Line.credit(LedgerAccounts.SALES_REVENUE, amount)));
                return record(result, payment, PaymentEventOutcome.PAID);
            }
        }
    }

    private PaymentEventOutcome onFailure(PaymentResult result, PaymentAttempt attempt, Payment payment) {
        if (attempt.getStatus() == AttemptStatus.PENDING) {
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setResultCode(result.resultCode());
        }
        if (payment.getStatus().isFinal()) {
            return record(result, payment, PaymentEventOutcome.IGNORED_FINAL);
        }
        return record(result, payment, PaymentEventOutcome.ATTEMPT_FAILED);
    }

    /** Integration events go through the outbox, in this same transaction. */
    private void publish(PaymentResult result, Payment payment, PaymentEventOutcome outcome) {
        if (payment == null) {
            return;
        }
        String orderId = String.valueOf(payment.getOrder().getId());
        if (outcome == PaymentEventOutcome.PAID) {
            outboxService.enqueue(PaymentEvents.AGGREGATE, orderId, PaymentEvents.PAYMENT_SUCCEEDED,
                    new PaymentEvents.PaymentSucceeded(payment.getOrder().getId(), payment.getId(), result.amount(),
                            result.transId(), payment.getPaidAt()));
        } else if (outcome.isAnomaly()) {
            outboxService.enqueue(PaymentEvents.AGGREGATE, orderId, PaymentEvents.PAYMENT_ANOMALY,
                    new PaymentEvents.PaymentAnomalyDetected(payment.getOrder().getId(), payment.getId(),
                            outcome.name(), result.amount(), result.transId(), result.providerOrderId()));
        }
    }

    /** Money really arrived but the sale is not valid: owe it back to the customer until refunded. */
    private void postRefundPayable(PaymentResult result, Payment payment, String reason) {
        long amount = result.amount();
        ledgerService.post(LEDGER_SOURCE, ledgerSourceId(result), "MoMo payment to refund for order #"
                + payment.getOrder().getId() + " (" + reason + ")", List.of(
                        Line.debit(LedgerAccounts.MOMO_RECEIVABLE, amount),
                        Line.credit(LedgerAccounts.REFUND_PAYABLE, amount)));
    }

    /** One ledger entry per MoMo transaction: transId is unique on MoMo's side. */
    private static String ledgerSourceId(PaymentResult result) {
        return result.transId() != null ? "trans:" + result.transId() : "order:" + result.providerOrderId();
    }

    private void markPaid(Payment payment, Long transId) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderTransId(transId);
        payment.setPaidAt(LocalDateTime.now(clock));

        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID.name());
        if ("PENDING".equals(order.getStatus())) {
            order.setStatus("CONFIRMED");
        }
    }

    private PaymentEventOutcome record(PaymentResult result, Payment payment, PaymentEventOutcome outcome) {
        publish(result, payment, outcome);
        eventRepository.save(PaymentEvent.builder()
                .payment(payment)
                .source(result.source())
                .providerOrderId(result.providerOrderId())
                .transId(result.transId())
                .amount(result.amount())
                .resultCode(result.resultCode())
                .signatureValid(true)
                .outcome(outcome)
                .payload(result.payload())
                .build());
        return outcome;
    }
}
