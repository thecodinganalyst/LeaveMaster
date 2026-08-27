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

class AssistantToolExecutionException extends RuntimeException {
    private final String conversationId;
    private final String toolName;

    AssistantToolExecutionException(String message, String conversationId, String toolName, Throwable cause) {
        super(message, cause);
        this.conversationId = conversationId;
        this.toolName = toolName;
    }

    String getConversationId() {
        return conversationId;
    }

    String getToolName() {
        return toolName;
    }
}
