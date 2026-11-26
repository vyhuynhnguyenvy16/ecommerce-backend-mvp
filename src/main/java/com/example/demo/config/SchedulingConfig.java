package com.example.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Background jobs (outbox relay, payment expiry, reconciliation). Tests switch it off to run jobs by hand. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "vestra.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
