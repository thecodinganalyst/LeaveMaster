package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantRateLimitServiceTest {
    private AssistantAuditEventRepository repository;
    private AssistantAuditService auditService;
    private AssistantRateLimitService service;

    @BeforeEach
    void setUp() {
        repository = mock(AssistantAuditEventRepository.class);
        auditService = mock(AssistantAuditService.class);
        service = new AssistantRateLimitService(repository, auditService);
        ReflectionTestUtils.setField(service, "userPerMinute", 2);
        ReflectionTestUtils.setField(service, "tenantPerMinute", 3);
        ReflectionTestUtils.setField(service, "maxMessageChars", 20);
    }

    @Test
    void shouldRecordAcceptedRequest() {
        service.checkAndRecord("dennis", "T1", "c1", "hello");
        verify(auditService).record(eq(AssistantAuditService.CHAT_REQUEST), eq("dennis"), eq("T1"), eq("c1"),
                eq(null), any(), eq("ACCEPTED"), eq(null));
    }

    @Test
    void shouldEnforceUserTenantAndMessageLimits() {
        when(repository.countByEventTypeAndActorLoginNameAndCreatedAtAfter(eq(AssistantAuditService.CHAT_REQUEST), eq("dennis"), any(Instant.class)))
                .thenReturn(2L);
        assertThatThrownBy(() -> service.checkAndRecord("dennis", "T1", "c1", "hello"))
                .isInstanceOf(AssistantRateLimitException.class).hasMessageContaining("user");

        when(repository.countByEventTypeAndActorLoginNameAndCreatedAtAfter(eq(AssistantAuditService.CHAT_REQUEST), eq("dennis"), any(Instant.class)))
                .thenReturn(0L);
        when(repository.countByEventTypeAndTenantIdAndCreatedAtAfter(eq(AssistantAuditService.CHAT_REQUEST), eq("T1"), any(Instant.class)))
                .thenReturn(3L);
        assertThatThrownBy(() -> service.checkAndRecord("dennis", "T1", "c1", "hello"))
                .isInstanceOf(AssistantRateLimitException.class).hasMessageContaining("tenant");

        assertThatThrownBy(() -> service.checkAndRecord("dennis", "T1", "c1", "this message is much too long"))
                .isInstanceOf(AssistantRateLimitException.class).hasMessageContaining("size");
    }
}
