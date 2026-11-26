package com.example.demo.outbox;

/** Sends the customer-facing "payment received" notice. The default implementation only logs. */
public interface NotificationSender {

    void paymentReceived(String orderId, long amount);
}
