package com.practical.leavemaster.assistant;

import java.util.concurrent.atomic.AtomicInteger;

final class AssistantRequestTrace {
    private final long startedAtNanos = System.nanoTime();
    private final AtomicInteger toolCallCount = new AtomicInteger();
    private final AtomicInteger toolFailureCount = new AtomicInteger();
    private volatile String lastStartedTool;
    private volatile String lastCompletedTool;
    private volatile String lastFailedTool;
    private volatile Throwable lastToolFailure;

    long elapsedMillis() {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    int toolStarted(String toolName) {
        lastStartedTool = toolName;
        return toolCallCount.incrementAndGet();
    }

    void toolCompleted(String toolName) {
        lastCompletedTool = toolName;
    }

    void toolFailed(String toolName) {
        toolFailed(toolName, null);
    }

    void toolFailed(String toolName, Throwable failure) {
        lastCompletedTool = toolName;
        lastFailedTool = toolName;
        lastToolFailure = failure;
        toolFailureCount.incrementAndGet();
    }

    int toolCallCount() {
        return toolCallCount.get();
    }

    int toolFailureCount() {
        return toolFailureCount.get();
    }

    String lastStartedTool() {
        return lastStartedTool == null ? "<none>" : lastStartedTool;
    }

    String lastCompletedTool() {
        return lastCompletedTool == null ? "<none>" : lastCompletedTool;
    }

    boolean hasToolFailure() {
        return lastFailedTool != null;
    }

    String lastFailedTool() {
        return lastFailedTool == null ? "<none>" : lastFailedTool;
    }

    Throwable lastToolFailure() {
        return lastToolFailure;
    }
}
