package com.practical.leavemaster.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
class AssistantProviderGuard {
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<Instant> openUntil = new AtomicReference<>();

    @Value("${app.assistant.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${app.assistant.circuit-breaker.open-seconds:30}")
    private long openSeconds;

    void beforeCall() {
        Instant until = openUntil.get();
        if (until != null && Instant.now().isBefore(until)) {
            throw new AssistantUnavailableException("AI provider circuit breaker is open");
        }
        if (until != null) {
            openUntil.compareAndSet(until, null);
            consecutiveFailures.set(0);
        }
    }

    void success() {
        consecutiveFailures.set(0);
        openUntil.set(null);
    }

    void failure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openUntil.set(Instant.now().plus(Duration.ofSeconds(openSeconds)));
        }
    }
}
