package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssistantAuditServiceTest {
    @Test
    void shouldRedactSecretsFromAuditArguments() {
        AssistantAuditEventRepository repository = mock(AssistantAuditEventRepository.class);
        AssistantAuditService service = new AssistantAuditService(repository, new ObjectMapper());

        service.record(AssistantAuditService.TOOL_EXECUTION, "dennis", "T1", "c1", "changePassword",
                Map.of("loginName", "mary", "password", "super-secret", "nested", Map.of("apiKey", "sk-test")),
                "SUCCESS", null);

        ArgumentCaptor<AssistantAuditEvent> captor = ArgumentCaptor.forClass(AssistantAuditEvent.class);
        verify(repository).save(captor.capture());
        String audited = captor.getValue().getSanitizedArguments();
        assertThat(audited).contains("mary", "[REDACTED]")
                .doesNotContain("super-secret")
                .doesNotContain("sk-test");
    }
}
