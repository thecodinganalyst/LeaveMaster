package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantProviderEnvironmentPostProcessorTest {

    private final AssistantProviderEnvironmentPostProcessor processor = new AssistantProviderEnvironmentPostProcessor();

    @Test
    void disablesSpringAiModelsWhenAssistantIsDisabled() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("ASSISTANT_ENABLED", "false")
            .withProperty("ASSISTANT_PROVIDER", "gemini");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");
    }

    @Test
    void selectsOpenAiWhenEnabled() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("ASSISTANT_ENABLED", "true")
            .withProperty("ASSISTANT_PROVIDER", " OPENAI ");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("openai");
    }

    @Test
    void mapsGeminiToGoogleGenAi() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("ASSISTANT_ENABLED", "true")
            .withProperty("ASSISTANT_PROVIDER", "GEMINI");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("google-genai");
    }

    @Test
    void preventsUnknownProviderFromInitializingAModel() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("ASSISTANT_ENABLED", "true")
            .withProperty("ASSISTANT_PROVIDER", "unknown");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");
    }

    @Test
    void defaultsToOpenAiProviderButRemainsDisabledByDefault() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");
        assertThat(processor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
    }
}
