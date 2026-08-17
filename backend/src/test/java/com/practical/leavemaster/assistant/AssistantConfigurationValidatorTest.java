package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssistantConfigurationValidatorTest {

    private static final DefaultApplicationArguments NO_ARGS = new DefaultApplicationArguments(new String[0]);

    @Test
    void disabledAssistantRequiresNoProviderCredentials() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(false, "invalid", "", "", "");
        assertThatCode(() -> validator.run(NO_ARGS)).doesNotThrowAnyException();
    }

    @Test
    void acceptsOpenAiConfiguration() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(true, " OPENAI ", "gpt-5-mini", "key", "");
        assertThatCode(() -> validator.run(NO_ARGS)).doesNotThrowAnyException();
    }

    @Test
    void acceptsGeminiConfiguration() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(true, "GEMINI", "gemini-2.5-flash", "", "key");
        assertThatCode(() -> validator.run(NO_ARGS)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedProvider() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(true, "other", "model", "key", "key");
        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("openai, gemini");
    }

    @Test
    void requiresModelWhenEnabled() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(true, "openai", " ", "key", "");
        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ASSISTANT_MODEL");
    }

    @Test
    void requiresOpenAiKeyOnlyForOpenAi() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(true, "openai", "gpt-5-mini", "", "unused");
        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void requiresGeminiKeyOnlyForGemini() {
        AssistantConfigurationValidator validator = new AssistantConfigurationValidator(true, "gemini", "gemini-2.5-flash", "unused", null);
        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GEMINI_API_KEY");
    }
}
