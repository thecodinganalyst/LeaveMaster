package com.practical.leavemaster.assistant;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssistantRetryObservabilityConfigurationTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AssistantRetryObservabilityConfiguration.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.put(AssistantRetryObservabilityConfiguration.MDC_CONVERSATION_ID, "conversation-retry");
        MDC.put(AssistantRetryObservabilityConfiguration.MDC_PROVIDER, "gemini");
        MDC.put(AssistantRetryObservabilityConfiguration.MDC_MODEL, "gemini-3.6-flash");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void shouldInstallListenerAndLogCorrelatedRetry() {
        RetryTemplate retryTemplate = new RetryTemplate(RetryPolicy.withMaxRetries(1));
        BeanPostProcessor postProcessor = AssistantRetryObservabilityConfiguration
                .assistantRetryTemplateObservabilityPostProcessor();
        assertThat(postProcessor.postProcessAfterInitialization(retryTemplate, "retryTemplate")).isSameAs(retryTemplate);

        AtomicInteger calls = new AtomicInteger();
        String result = retryTemplate.invoke(() -> {
            if (calls.getAndIncrement() == 0) throw new IllegalStateException("transient failure");
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(2);
        assertThat(formattedLogs())
                .contains("Ask LeaveMaestro provider retry")
                .contains("provider=gemini")
                .contains("model=gemini-3.6-flash")
                .contains("conversationId=conversation-retry")
                .contains("previousExceptionType=java.lang.IllegalStateException");
    }

    @Test
    void shouldLogWhenRetryPolicyIsExhausted() {
        RetryTemplate retryTemplate = new RetryTemplate(RetryPolicy.withMaxRetries(1));
        AssistantRetryObservabilityConfiguration.installAssistantRetryListener(retryTemplate);

        assertThatThrownBy(() -> retryTemplate.invoke(() -> {
            throw new IllegalStateException("still unavailable");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(formattedLogs())
                .contains("Ask LeaveMaestro provider retries exhausted")
                .contains("provider=gemini")
                .contains("model=gemini-3.6-flash")
                .contains("conversationId=conversation-retry")
                .contains("lastExceptionType=java.lang.IllegalStateException");
    }

    private String formattedLogs() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
    }
}
