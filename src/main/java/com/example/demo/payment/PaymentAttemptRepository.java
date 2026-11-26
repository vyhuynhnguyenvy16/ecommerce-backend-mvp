package com.example.demo.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByProviderOrderId(String providerOrderId);

    Optional<PaymentAttempt> findFirstByPaymentIdOrderByCreatedAtDescIdDesc(Long paymentId);

    List<PaymentAttempt> findByPaymentIdAndStatus(Long paymentId, AttemptStatus status);

    /** Pending attempts of still-open payments that have waited longer than {@code before} (lost IPN candidates). */
    @Query("""
            select a from PaymentAttempt a
            where a.status = com.example.demo.payment.AttemptStatus.PENDING
              and a.payment.status = com.example.demo.payment.PaymentStatus.PENDING
              and a.createdAt <= :before
            order by a.id
            """)
    List<PaymentAttempt> findStalePending(@Param("before") LocalDateTime before);
}
