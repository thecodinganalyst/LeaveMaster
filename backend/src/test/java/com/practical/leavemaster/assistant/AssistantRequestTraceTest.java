package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantRequestTraceTest {

    @Test
    void shouldTrackSuccessfulAndFailedToolCalls() {
        AssistantRequestTrace trace = new AssistantRequestTrace();

        assertThat(trace.toolCallCount()).isZero();
        assertThat(trace.toolFailureCount()).isZero();
        assertThat(trace.lastStartedTool()).isEqualTo("<none>");
        assertThat(trace.lastCompletedTool()).isEqualTo("<none>");
        assertThat(trace.hasToolFailure()).isFalse();
        assertThat(trace.lastFailedTool()).isEqualTo("<none>");
        assertThat(trace.lastToolFailure()).isNull();
        assertThat(trace.elapsedMillis()).isGreaterThanOrEqualTo(0L);

        assertThat(trace.toolStarted("getStaffById")).isEqualTo(1);
        trace.toolCompleted("getStaffById");

        assertThat(trace.lastStartedTool()).isEqualTo("getStaffById");
        assertThat(trace.lastCompletedTool()).isEqualTo("getStaffById");
        assertThat(trace.hasToolFailure()).isFalse();

        RuntimeException failure = new RuntimeException("read failed");
        assertThat(trace.toolStarted("getLeaveBalances")).isEqualTo(2);
        trace.toolFailed("getLeaveBalances", failure);

        assertThat(trace.toolCallCount()).isEqualTo(2);
        assertThat(trace.toolFailureCount()).isEqualTo(1);
        assertThat(trace.lastCompletedTool()).isEqualTo("getLeaveBalances");
        assertThat(trace.hasToolFailure()).isTrue();
        assertThat(trace.lastFailedTool()).isEqualTo("getLeaveBalances");
        assertThat(trace.lastToolFailure()).isSameAs(failure);
    }
}
