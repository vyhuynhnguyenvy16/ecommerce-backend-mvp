package com.example.demo.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    List<PaymentEvent> findByProviderOrderIdOrderByIdAsc(String providerOrderId);

    List<PaymentEvent> findByOutcomeIn(List<PaymentEventOutcome> outcomes);
}
