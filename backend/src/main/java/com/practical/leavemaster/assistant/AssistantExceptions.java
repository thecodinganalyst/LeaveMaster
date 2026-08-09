package com.practical.leavemaster.assistant;

class AssistantUnavailableException extends RuntimeException {
    AssistantUnavailableException(String message) {
        super(message);
    }
}

class AssistantProviderException extends RuntimeException {
    AssistantProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
