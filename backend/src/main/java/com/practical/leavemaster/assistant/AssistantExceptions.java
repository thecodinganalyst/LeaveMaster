package com.practical.leavemaster.assistant;

class AssistantUnavailableException extends RuntimeException {
    AssistantUnavailableException(String message) {
        super(message);
    }
}

class AssistantProviderException extends RuntimeException {
    private final String conversationId;

    AssistantProviderException(String message, Throwable cause) {
        this(message, null, cause);
    }

    AssistantProviderException(String message, String conversationId, Throwable cause) {
        super(message, cause);
        this.conversationId = conversationId;
    }

    String getConversationId() {
        return conversationId;
    }
}
