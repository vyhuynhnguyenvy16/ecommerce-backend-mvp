package com.example.demo.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.demo.payment.momo.MomoClient;
import com.example.demo.payment.momo.MomoGatewayException;
import com.example.demo.payment.momo.MomoResultCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Asks MoMo for the real status of an attempt (POST /v2/gateway/api/query) and feeds a real payment outcome through
 * the same {@link PaymentResultProcessor} as the IPN. Deliberately NOT transactional: the HTTP call must not hold a
 * database lock; the processor opens its own transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusQuerier {

    public enum Verdict {
        /** MoMo returned a payment outcome and it was applied. */
        APPLIED,
        /** MoMo has no transaction for this orderId: nothing was paid with this link. */
        NOT_FOUND,
        /** MoMo could not be asked or refused the request: the true state is unknown. */
        UNCERTAIN
    }

    private final MomoClient momoClient;
    private final PaymentResultProcessor processor;

    public Verdict queryAndApply(PaymentAttempt attempt) {
        MomoClient.QueryResponse response;
        try {
            response = momoClient.queryPayment(attempt.getProviderOrderId(), UUID.randomUUID().toString());
        } catch (MomoGatewayException e) {
            log.warn("MoMo query failed for {}: {}", attempt.getProviderOrderId(), e.getMessage());
            return Verdict.UNCERTAIN;
        }
        if (response == null || response.resultCode() == null) {
            return Verdict.UNCERTAIN;
        }
        int code = response.resultCode();
        if (code == MomoResultCodes.ORDER_NOT_FOUND) {
            return Verdict.NOT_FOUND;
        }
        if (MomoResultCodes.isRequestRejection(code)) {
            log.warn("MoMo rejected query for {} with resultCode {}", attempt.getProviderOrderId(), code);
            return Verdict.UNCERTAIN;
        }
        Long amount = response.amount() != null ? response.amount() : attempt.getAmount();
        processor.apply(new PaymentResult(PaymentEvent.Source.QUERY, attempt.getProviderOrderId(), amount,
                response.transId(), code, null));
        return Verdict.APPLIED;
    }
}
