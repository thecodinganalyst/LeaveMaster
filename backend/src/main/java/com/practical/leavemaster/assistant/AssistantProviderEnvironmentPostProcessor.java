package com.practical.leavemaster.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Maps LeaveMaster's provider-neutral environment variables to Spring AI's model selector.
 * This runs before model auto-configuration so only the selected provider is initialized.
 */
public class AssistantProviderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "leaveMasterAssistantProvider";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = Boolean.parseBoolean(environment.getProperty("ASSISTANT_ENABLED", "false"));
        String provider = environment.getProperty("ASSISTANT_PROVIDER", "openai").trim().toLowerCase();

        String springAiProvider = enabled ? switch (provider) {
            case "openai" -> "openai";
            case "gemini" -> "google-genai";
            default -> "none";
        } : "none";

        environment.getPropertySources().addFirst(new MapPropertySource(
            PROPERTY_SOURCE_NAME,
            Map.of("spring.ai.model.chat", springAiProvider)
        ));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
