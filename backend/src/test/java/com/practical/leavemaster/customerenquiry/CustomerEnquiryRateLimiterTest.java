package com.practical.leavemaster.customerenquiry;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEnquiryRateLimiterTest {

    @Test
    void shouldLimitRequestsWithinWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
        CustomerEnquiryRateLimiter limiter = new CustomerEnquiryRateLimiter(2, Duration.ofMinutes(15), clock);

        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isFalse();
        assertThat(limiter.tryAcquire("client-b")).isTrue();
        assertThat(limiter.tryAcquire(null)).isTrue();
    }
}
