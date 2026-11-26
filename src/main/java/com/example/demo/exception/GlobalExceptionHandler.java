package com.example.demo.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.payment.PaymentNotAllowedException;
import com.example.demo.payment.momo.MomoGatewayException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Sản phẩm vừa được cập nhật bởi người khác, vui lòng thử lại"));
    }

    @ExceptionHandler(PaymentNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotAllowed(PaymentNotAllowedException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MomoGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleMomoGateway(MomoGatewayException ex) {
        // The customer gets a generic message; operators need the real cause (resultCode, network error…).
        log.warn("MoMo gateway error (resultCode {}): {}", ex.getResultCode(), ex.getMessage(), ex.getCause());
        return error(HttpStatus.BAD_GATEWAY, "Không kết nối được MoMo, vui lòng thử lại sau");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(body);
    }
}
