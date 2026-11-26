package com.example.demo.payment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payment.dto.PaymentLinkResponse;
import com.example.demo.payment.dto.PaymentStatusResponse;
import com.example.demo.payment.momo.MomoClient;
import com.example.demo.payment.momo.MomoProperties;
import com.example.demo.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final OrderRepository orderRepository;
    private final MomoClient momoClient;
    private final MomoProperties momoProperties;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    /**
     * Called inside the checkout transaction for a MoMo order: records the payment and starts the expiry window.
     * Validates the amount before any money-related state is written.
     */
    @Transactional
    public Payment openMomoPayment(Order order) {
        if (!momoProperties.isConfigured()) {
            throw new PaymentNotAllowedException("Thanh toán MoMo tạm thời chưa khả dụng");
        }
        MomoAmount.toVnd(order.getTotalAmount());
        Payment payment = Payment.builder()
                .order(order)
                .method(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .idempotencyKey("momo:order:" + order.getId())
                .expiresAt(now().plus(paymentProperties.momoExpiry()))
                .build();
        return paymentRepository.save(payment);
    }

    /**
     * Returns a MoMo payUrl for the customer's order. Reuses the latest pending attempt so a double click or a
     * page refresh never creates two payable links; a new attempt (new MoMo orderId) is created only when there is
     * none yet or the previous one failed. The payment row is locked for the whole call, which serialises
     * concurrent requests for the same order. The MoMo call happens inside the lock on purpose (max ~35s).
     */
    @Transactional
    public PaymentLinkResponse startMomoAttempt(Long userId, Long orderId) {
        Order order = loadOwnedOrder(userId, orderId);
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentNotAllowedException("Đơn hàng này không thanh toán qua MoMo"));

        if (payment.getStatus() != PaymentStatus.PENDING || !"PENDING".equals(order.getStatus())) {
            throw new PaymentNotAllowedException("Đơn hàng không còn chờ thanh toán");
        }
        if (!now().isBefore(payment.getExpiresAt())) {
            throw new PaymentNotAllowedException("Đơn hàng đã hết hạn thanh toán");
        }

        Optional<PaymentAttempt> latest = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDescIdDesc(payment.getId());
        if (latest.isPresent() && latest.get().getStatus() == AttemptStatus.PENDING && latest.get().getPayUrl() != null) {
            return new PaymentLinkResponse(orderId, latest.get().getPayUrl(), payment.getExpiresAt());
        }

        long amount = MomoAmount.toVnd(payment.getAmount());
        String providerOrderId = "VESTRA-" + orderId + "-" + Instant.now(clock).toEpochMilli();
        String requestId = UUID.randomUUID().toString();
        String redirectUrl = momoProperties.redirectUrl() + "?orderId=" + orderId;

        MomoClient.CreateResponse response = momoClient.createPayment(providerOrderId, requestId, amount,
                "VESTRA order #" + orderId, redirectUrl);

        attemptRepository.save(PaymentAttempt.builder()
                .payment(payment)
                .providerOrderId(providerOrderId)
                .requestId(requestId)
                .amount(amount)
                .status(AttemptStatus.PENDING)
                .payUrl(response.payUrl())
                .build());

        return new PaymentLinkResponse(orderId, response.payUrl(), payment.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(Long userId, Long orderId) {
        Order order = loadOwnedOrder(userId, orderId);
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        if (payment.isEmpty()) {
            return new PaymentStatusResponse(orderId, order.getStatus(), PaymentMethod.valueOf(order.getPaymentMethod()),
                    PaymentStatus.valueOf(order.getPaymentStatus()), order.getTotalAmount(), null, null, null, null,
                    false);
        }
        Payment p = payment.get();
        Optional<PaymentAttempt> latest = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDescIdDesc(p.getId());
        boolean canRetry = p.getStatus() == PaymentStatus.PENDING && "PENDING".equals(order.getStatus())
                && now().isBefore(p.getExpiresAt());
        return new PaymentStatusResponse(orderId, order.getStatus(), p.getMethod(), p.getStatus(), p.getAmount(),
                p.getExpiresAt(), p.getPaidAt(),
                latest.map(PaymentAttempt::getStatus).orElse(null),
                latest.map(PaymentAttempt::getResultCode).orElse(null),
                canRetry);
    }

    private Order loadOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this order");
        }
        return order;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
