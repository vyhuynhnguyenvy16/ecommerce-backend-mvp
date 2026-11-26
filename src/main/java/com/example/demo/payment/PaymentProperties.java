package com.example.demo.payment;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** VESTRA payment rules. */
@ConfigurationProperties(prefix = "vestra.payments")
public record PaymentProperties(
        /** How long a MoMo order keeps its reserved stock before it is expired. */
        @DefaultValue("15m") Duration momoExpiry) {
}
