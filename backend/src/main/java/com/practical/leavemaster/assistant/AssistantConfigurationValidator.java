package com.practical.leavemaster.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AssistantConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AssistantConfigurationValidator.class);

    private final boolean enabled;
    private final String provider;
    private final String model;
    private final String openAiApiKey;
    private final String geminiApiKey;

    public AssistantConfigurationValidator(
        @Value("${app.assistant.enabled:false}") boolean enabled,
        @Value("${app.assistant.provider:openai}") String provider,
        @Value("${app.assistant.model:}") String model,
        @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
        @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey
    ) {
        this.enabled = enabled;
        this.provider = normalize(provider);
        this.model = model == null ? "" : model.trim();
        this.openAiApiKey = openAiApiKey;
        this.geminiApiKey = geminiApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Ask LeaveMaestro is disabled; no AI chat provider will be initialized");
            return;
        }

        if (!"openai".equals(provider) && !"gemini".equals(provider)) {
            throw new IllegalStateException("ASSISTANT_PROVIDER must be one of: openai, gemini");
        }
        if (model.isBlank()) {
            throw new IllegalStateException("ASSISTANT_ENABLED=true requires ASSISTANT_MODEL to be configured");
        }

        if ("openai".equals(provider) && isBlank(openAiApiKey)) {
            throw new IllegalStateException("ASSISTANT_PROVIDER=openai requires OPENAI_API_KEY");
        }
        if ("gemini".equals(provider) && isBlank(geminiApiKey)) {
            throw new IllegalStateException("ASSISTANT_PROVIDER=gemini requires GEMINI_API_KEY");
        }

        log.info("Ask LeaveMaestro enabled with provider={} model={}", provider, model);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
