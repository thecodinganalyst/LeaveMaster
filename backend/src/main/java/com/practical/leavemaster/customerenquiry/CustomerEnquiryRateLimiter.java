package com.practical.leavemaster.customerenquiry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomerEnquiryRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public CustomerEnquiryRateLimiter(
            @Value("${app.customer-enquiry.rate-limit.max-requests:5}") int maxRequests,
            @Value("${app.customer-enquiry.rate-limit.window-seconds:900}") long windowSeconds) {
        this(maxRequests, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    CustomerEnquiryRateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    public boolean tryAcquire(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        Instant now = clock.instant();
        WindowCounter updated = counters.compute(key, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.windowStart().plus(window))) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(existing.windowStart(), existing.count() + 1);
        });
        return updated.count() <= maxRequests;
    }

    record WindowCounter(Instant windowStart, int count) {
    }
}
