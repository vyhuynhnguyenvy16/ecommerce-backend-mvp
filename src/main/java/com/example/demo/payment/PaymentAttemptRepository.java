package com.example.demo.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByProviderOrderId(String providerOrderId);

    Optional<PaymentAttempt> findFirstByPaymentIdOrderByCreatedAtDescIdDesc(Long paymentId);

    List<PaymentAttempt> findByPaymentIdAndStatus(Long paymentId, AttemptStatus status);
}
