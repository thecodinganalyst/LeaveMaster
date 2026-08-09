package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantToolAdapterTest {

    @Test
    void shouldExecuteAuthorizedReadToolNormallyAuditItAndExposeStructuredResult() {
        ToolCallback read = callback("getLeaveBalances");
        when(read.call("{}" )).thenReturn("[{\"leaveType\":\"Annual\",\"balance\":12.5}]");
        var authentication = authentication(RbacPermissions.LEAVE_APPLICATION_READ);
        AssistantAuditService audit = mock(AssistantAuditService.class);
        List<AssistantDtos.StructuredResult> results = new ArrayList<>();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication, user(), new ObjectMapper(), new ArrayList<>(), results,
                "c1", mock(AssistantConfirmationService.class), audit);

        assertThat(adapted).hasSize(1);
        assertThat(adapted[0].call("{}")).contains("Annual");
        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.toolName()).isEqualTo("getLeaveBalances");
            assertThat(result.data()).asList().singleElement().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                    .containsEntry("leaveType", "Annual")
                    .containsEntry("balance", 12.5);
        });
        verify(read).call("{}");
        verify(audit).record(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void shouldKeepPlainTextReadResultsStructuredWithoutFailing() {
        ToolCallback read = callback("getTenantById");
        when(read.call("{}" )).thenReturn("not-json");
        List<AssistantDtos.StructuredResult> results = new ArrayList<>();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication(RbacPermissions.TENANT_READ), user(), new ObjectMapper(),
                new ArrayList<>(), results, "c1", mock(AssistantConfirmationService.class), mock(AssistantAuditService.class));

        adapted[0].call("{}");
        assertThat(results).singleElement().satisfies(result -> assertThat(result.data()).isEqualTo("not-json"));
    }

    @Test
    void shouldTurnAuthorizedWriteIntoPersistedPendingActionWithoutExecutingIt() {
        ToolCallback write = callback("createTenant");
        var authentication = authentication(RbacPermissions.TENANT_WRITE);
        List<AssistantDtos.PendingAction> pending = new ArrayList<>();
        AssistantConfirmationService confirmations = mock(AssistantConfirmationService.class);
        var pendingAction = new AssistantDtos.PendingAction("createTenant", Map.of("id", "T1"),
                RbacPermissions.TENANT_WRITE, "dennis", "S1", "tenant-1", "token-1", Instant.now().plusSeconds(300));
        when(confirmations.issue(anyString(), any(), anyString(), any(), anyString())).thenReturn(pendingAction);

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{write}, authentication, user(), new ObjectMapper(), pending,
                "c1", confirmations, mock(AssistantAuditService.class));

        String input = "{\"id\":\"T1\",\"name\":\"Tenant One\"}";
        assertThat(adapted[0].call(input)).contains("confirmation");
        assertThat(pending).containsExactly(pendingAction);
        verify(write, never()).call(input);
    }

    @Test
    void shouldNotExposeToolWithoutRequiredAuthority() {
        ToolCallback write = callback("deleteTenant");
        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{write}, authentication(RbacPermissions.TENANT_READ), user(), new ObjectMapper(), new ArrayList<>(),
                "c1", mock(AssistantConfirmationService.class), mock(AssistantAuditService.class));
        assertThat(adapted).isEmpty();
    }

    private ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name).description(name).inputSchema("{\"type\":\"object\"}").build());
        when(callback.getToolMetadata()).thenReturn(ToolMetadata.builder().build());
        return callback;
    }

    private UsernamePasswordAuthenticationToken authentication(String authority) {
        return new UsernamePasswordAuthenticationToken("dennis", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }

    private AppUser user() {
        return AppUser.builder().loginName("dennis").staffId("S1").tenantId("tenant-1").active(true).build();
    }
}
