package com.example.demo.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.payment.dto.PaymentLinkResponse;
import com.example.demo.payment.dto.PaymentStatusResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Get (or reuse) a MoMo payUrl for one of the caller's pending MoMo orders. */
    @PostMapping("/api/orders/{orderId}/payments/momo")
    public ResponseEntity<PaymentLinkResponse> startMomo(Authentication authentication, @PathVariable Long orderId) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(paymentService.startMomoAttempt(userId, orderId));
    }

    /** Current payment state; polled by the storefront after MoMo redirects back. */
    @GetMapping("/api/orders/{orderId}/payment")
    public ResponseEntity<PaymentStatusResponse> status(Authentication authentication, @PathVariable Long orderId) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(paymentService.getStatus(userId, orderId));
    }
}
