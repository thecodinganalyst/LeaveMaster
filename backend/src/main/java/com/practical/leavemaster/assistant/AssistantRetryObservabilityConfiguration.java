package com.practical.leavemaster.assistant;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.core.retry.support.CompositeRetryListener;

import java.util.List;

@Slf4j
@Configuration(proxyBeanMethods = false)
class AssistantRetryObservabilityConfiguration {

    static final String MDC_CONVERSATION_ID = "assistantConversationId";
    static final String MDC_PROVIDER = "assistantProvider";
    static final String MDC_MODEL = "assistantModel";

    @Bean
    static BeanPostProcessor assistantRetryTemplateObservabilityPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RetryTemplate retryTemplate) {
                    installAssistantRetryListener(retryTemplate);
                }
                return bean;
            }
        };
    }

    static void installAssistantRetryListener(RetryTemplate retryTemplate) {
        RetryListener assistantListener = new AssistantRetryListener();
        RetryListener existing = retryTemplate.getRetryListener();
        if (existing == null) {
            retryTemplate.setRetryListener(assistantListener);
            return;
        }
        retryTemplate.setRetryListener(new CompositeRetryListener(List.of(existing, assistantListener)));
    }

    private static final class AssistantRetryListener implements RetryListener {
        @Override
        public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
            String conversationId = MDC.get(MDC_CONVERSATION_ID);
            if (conversationId == null || conversationId.isBlank()) return;

            Throwable lastFailure = retryState.getLastException();
            log.warn(
                    "Ask LeaveMaestro provider retry: provider={}, model={}, conversationId={}, retryNumber={}, previousExceptionType={}",
                    mdcOrUnknown(MDC_PROVIDER),
                    mdcOrUnknown(MDC_MODEL),
                    conversationId,
                    retryState.getRetryCount(),
                    lastFailure == null ? "<unknown>" : lastFailure.getClass().getName());
        }

        @Override
        public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
            String conversationId = MDC.get(MDC_CONVERSATION_ID);
            if (conversationId == null || conversationId.isBlank()) return;

            Throwable lastFailure = exception.getCause();
            log.warn(
                    "Ask LeaveMaestro provider retries exhausted: provider={}, model={}, conversationId={}, retryCount={}, lastExceptionType={}",
                    mdcOrUnknown(MDC_PROVIDER),
                    mdcOrUnknown(MDC_MODEL),
                    conversationId,
                    exception.getRetryCount(),
                    lastFailure == null ? "<unknown>" : lastFailure.getClass().getName());
        }
    }

    private static String mdcOrUnknown(String key) {
        String value = MDC.get(key);
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
