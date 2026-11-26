package com.example.demo.payment;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.entity.Order;
import com.example.demo.outbox.OutboxService;
import com.example.demo.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Expires MoMo payments whose window has passed: cancels the order and returns the stock.
 * Rule: never cancel without asking MoMo first. An IPN may have been lost, so every pending attempt is queried; if
 * any answer is uncertain the payment is left alone and retried on the next run.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExpiryService {

    public record Result(int expired, int paidOnQuery, int skippedUncertain) {
    }

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentStatusQuerier querier;
    private final OrderService orderService;
    private final OutboxService outboxService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public Result expireDuePayments() {
        int expired = 0;
        int paid = 0;
        int skipped = 0;
        for (Long paymentId : paymentRepository.findDueForExpiry(LocalDateTime.now(clock))) {
            boolean uncertain = false;
            for (PaymentAttempt attempt : attemptRepository.findByPaymentIdAndStatus(paymentId, AttemptStatus.PENDING)) {
                if (querier.queryAndApply(attempt) == PaymentStatusQuerier.Verdict.UNCERTAIN) {
                    uncertain = true;
                }
            }
            if (uncertain) {
                skipped++;
                continue;
            }
            PaymentStatus finalStatus = transactionTemplate.execute(status -> expireIfStillPending(paymentId));
            if (finalStatus == PaymentStatus.EXPIRED) {
                expired++;
            } else if (finalStatus == PaymentStatus.PAID) {
                paid++;
            }
        }
        if (expired + paid + skipped > 0) {
            log.info("Payment expiry run: {} expired, {} found paid, {} skipped (MoMo uncertain)", expired, paid, skipped);
        }
        return new Result(expired, paid, skipped);
    }

    private PaymentStatus expireIfStillPending(Long paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment.getStatus();
        }
        Order order = payment.getOrder();
        payment.setStatus(PaymentStatus.EXPIRED);
        order.setPaymentStatus(PaymentStatus.EXPIRED.name());
        if ("PENDING".equals(order.getStatus())) {
            order.setStatus("CANCELLED");
            orderService.restoreStock(order);
        }
        outboxService.enqueue(PaymentEvents.AGGREGATE, String.valueOf(order.getId()), PaymentEvents.PAYMENT_EXPIRED,
                new PaymentEvents.PaymentExpired(order.getId(), payment.getId(), LocalDateTime.now(clock)));
        return PaymentStatus.EXPIRED;
    }
}
