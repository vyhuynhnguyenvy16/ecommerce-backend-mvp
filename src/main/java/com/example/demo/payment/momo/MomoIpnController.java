package com.example.demo.payment.momo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.payment.PaymentEvent;
import com.example.demo.payment.PaymentEventOutcome;
import com.example.demo.payment.PaymentEventRepository;
import com.example.demo.payment.PaymentResult;
import com.example.demo.payment.PaymentResultProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * MoMo IPN endpoint (server-to-server, public). Per the MoMo docs the merchant validates the signature and
 * checks partnerCode, orderId and amount before trusting the result, and must answer HTTP 204 with no body
 * within 15 seconds. Unverified requests get 400 and change nothing.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MomoIpnController {

    public static final String PATH = "/api/payments/momo/ipn";

    private final MomoSigner signer;
    private final MomoProperties properties;
    private final PaymentResultProcessor processor;
    private final PaymentEventRepository eventRepository;
    private final JsonMapper jsonMapper;

    @PostMapping(PATH)
    public ResponseEntity<Void> receive(@RequestBody MomoIpnRequest ipn) {
        String payload = jsonMapper.writeValueAsString(ipn);

        if (!signer.verify(signer.ipnRawSignature(ipn), ipn.signature())) {
            log.warn("Rejected MoMo IPN with invalid signature for orderId {}", ipn.orderId());
            reject(ipn, payload, PaymentEventOutcome.INVALID_SIGNATURE);
            return ResponseEntity.badRequest().build();
        }
        if (!properties.partnerCode().equals(ipn.partnerCode())) {
            reject(ipn, payload, PaymentEventOutcome.WRONG_PARTNER);
            return ResponseEntity.badRequest().build();
        }
        if (ipn.resultCode() == null || ipn.orderId() == null) {
            reject(ipn, payload, PaymentEventOutcome.INVALID_SIGNATURE);
            return ResponseEntity.badRequest().build();
        }

        processor.apply(new PaymentResult(PaymentEvent.Source.IPN, ipn.orderId(), ipn.amount(), ipn.transId(),
                ipn.resultCode(), payload));
        return ResponseEntity.noContent().build();
    }

    private void reject(MomoIpnRequest ipn, String payload, PaymentEventOutcome outcome) {
        eventRepository.save(PaymentEvent.builder()
                .source(PaymentEvent.Source.IPN)
                .providerOrderId(ipn.orderId())
                .transId(ipn.transId())
                .amount(ipn.amount())
                .resultCode(ipn.resultCode())
                .signatureValid(outcome != PaymentEventOutcome.INVALID_SIGNATURE)
                .outcome(outcome)
                .payload(payload)
                .build());
    }
}
