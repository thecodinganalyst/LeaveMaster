package com.practical.leavemaster.assistant;

import java.util.concurrent.atomic.AtomicInteger;

final class AssistantRequestTrace {
    private final long startedAtNanos = System.nanoTime();
    private final AtomicInteger toolCallCount = new AtomicInteger();
    private volatile String lastStartedTool;
    private volatile String lastCompletedTool;

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

    int toolCallCount() {
        return toolCallCount.get();
    }

    String lastStartedTool() {
        return lastStartedTool == null ? "<none>" : lastStartedTool;
    }

    String lastCompletedTool() {
        return lastCompletedTool == null ? "<none>" : lastCompletedTool;
    }
}
