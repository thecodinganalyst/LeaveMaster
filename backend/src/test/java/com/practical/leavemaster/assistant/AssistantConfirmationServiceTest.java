package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantConfirmationServiceTest {
    private AssistantPendingActionRepository repository;
    private ToolCallbackProvider toolProvider;
    private AppUserRepository userRepository;
    private AssistantConfirmationService service;
    private ToolCallback callback;

    @BeforeEach
    void setUp() {
        repository = mock(AssistantPendingActionRepository.class);
        toolProvider = mock(ToolCallbackProvider.class);
        userRepository = mock(AppUserRepository.class);
        callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder().name("applyForLeave")
                .description("apply").inputSchema("{\"type\":\"object\"}").build());
        when(toolProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{callback});
        service = new AssistantConfirmationService(repository, toolProvider, userRepository,
                mock(AssistantAuditService.class), new ObjectMapper());
        ReflectionTestUtils.setField(service, "confirmationTtlSeconds", 300L);
        when(userRepository.findById("dennis")).thenReturn(Optional.of(user("dennis", "T1")));
    }

    @Test
    void shouldExecuteExactStoredArgumentsAndMakeReplayIdempotent() {
        AssistantPendingAction action = pending("dennis", "T1", "PENDING");
        when(repository.findByTokenForUpdate("token-1")).thenReturn(Optional.of(action));
        when(callback.call(action.getArgumentsJson())).thenReturn("created");

        var first = service.confirm("token-1", auth(RbacPermissions.LEAVE_APPLICATION_WRITE));
        assertThat(first.status()).isEqualTo("EXECUTED");
        assertThat(first.replayed()).isFalse();
        verify(callback).call("{\"staffId\":\"S1\",\"tenantId\":\"T1\"}");

        var replay = service.confirm("token-1", auth(RbacPermissions.LEAVE_APPLICATION_WRITE));
        assertThat(replay.replayed()).isTrue();
    }

    @Test
    void shouldRejectDifferentActorTenantAndLostAuthority() {
        AssistantPendingAction action = pending("mary", "T1", "PENDING");
        when(repository.findByTokenForUpdate("token-1")).thenReturn(Optional.of(action));
        assertThatThrownBy(() -> service.confirm("token-1", auth(RbacPermissions.LEAVE_APPLICATION_WRITE)))
                .isInstanceOf(AccessDeniedException.class);

        action.setActorLoginName("dennis");
        action.setTenantId("T2");
        assertThatThrownBy(() -> service.confirm("token-1", auth(RbacPermissions.LEAVE_APPLICATION_WRITE)))
                .isInstanceOf(AccessDeniedException.class);

        action.setTenantId("T1");
        assertThatThrownBy(() -> service.confirm("token-1", auth(RbacPermissions.LEAVE_APPLICATION_READ)))
                .isInstanceOf(AccessDeniedException.class);
        verify(callback, never()).call(any(String.class));
    }

    @Test
    void shouldRejectCrossTenantProposalAndExpiredToken() {
        assertThatThrownBy(() -> service.issue("applyForLeave", Map.of("tenantId", "T2"),
                RbacPermissions.LEAVE_APPLICATION_WRITE, user("dennis", "T1"), "c1"))
                .isInstanceOf(AccessDeniedException.class);

        AssistantPendingAction action = pending("dennis", "T1", "PENDING");
        action.setExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findByTokenForUpdate("token-1")).thenReturn(Optional.of(action));
        assertThatThrownBy(() -> service.confirm("token-1", auth(RbacPermissions.LEAVE_APPLICATION_WRITE)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expired");
        assertThat(action.getStatus()).isEqualTo("EXPIRED");
    }

    private AssistantPendingAction pending(String actor, String tenant, String status) {
        return AssistantPendingAction.builder().confirmationToken("token-1").toolName("applyForLeave")
                .argumentsJson("{\"staffId\":\"S1\",\"tenantId\":\"T1\"}")
                .requiredAuthority(RbacPermissions.LEAVE_APPLICATION_WRITE).actorLoginName(actor)
                .actorStaffId("S1").tenantId(tenant).conversationId("c1").status(status)
                .createdAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
    }

    private AppUser user(String login, String tenant) {
        return AppUser.builder().loginName(login).staffId("S1").tenantId(tenant).active(true).build();
    }

    private UsernamePasswordAuthenticationToken auth(String authority) {
        return new UsernamePasswordAuthenticationToken("dennis", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }
}
