package com.practical.leavemaster.assistant;

class AssistantRateLimitException extends RuntimeException {
    AssistantRateLimitException(String message) {
        super(message);
    }
}
