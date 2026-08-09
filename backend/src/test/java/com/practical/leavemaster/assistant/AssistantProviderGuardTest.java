package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssistantProviderGuardTest {
    @Test
    void shouldOpenAfterConfiguredFailuresAndResetOnSuccess() {
        AssistantProviderGuard guard = new AssistantProviderGuard();
        ReflectionTestUtils.setField(guard, "failureThreshold", 2);
        ReflectionTestUtils.setField(guard, "openSeconds", 30L);

        guard.beforeCall();
        guard.failure();
        guard.beforeCall();
        guard.failure();

        assertThatThrownBy(guard::beforeCall)
                .isInstanceOf(AssistantUnavailableException.class)
                .hasMessageContaining("circuit breaker");

        guard.success();
        guard.beforeCall();
    }
}
