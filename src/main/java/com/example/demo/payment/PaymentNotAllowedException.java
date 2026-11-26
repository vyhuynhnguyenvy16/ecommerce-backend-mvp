package com.example.demo.payment;

/** The requested payment action is not valid for the order's current state (HTTP 409). */
public class PaymentNotAllowedException extends RuntimeException {

    public PaymentNotAllowedException(String message) {
        super(message);
    }
}
