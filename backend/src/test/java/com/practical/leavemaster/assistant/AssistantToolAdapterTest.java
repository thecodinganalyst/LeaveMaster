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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        AssistantRequestTrace trace = new AssistantRequestTrace();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication, user(), new ObjectMapper(), new ArrayList<>(), results,
                "c1", mock(AssistantConfirmationService.class), audit, trace);

        assertThat(adapted).hasSize(1);
        assertThat(adapted[0].call("{}")).contains("Annual");
        assertThat(results).hasSize(1);
        AssistantDtos.StructuredResult result = results.getFirst();
        assertThat(result.toolName()).isEqualTo("getLeaveBalances");
        assertThat(result.data()).isInstanceOf(List.class);
        Object first = ((List<?>) result.data()).getFirst();
        assertThat(first).isInstanceOf(Map.class);
        Map<?, ?> firstMap = (Map<?, ?>) first;
        assertThat(firstMap.get("leaveType")).isEqualTo("Annual");
        assertThat(firstMap.get("balance")).isEqualTo(12.5);
        assertThat(trace.toolCallCount()).isEqualTo(1);
        assertThat(trace.lastStartedTool()).isEqualTo("getLeaveBalances");
        assertThat(trace.lastCompletedTool()).isEqualTo("getLeaveBalances");
        assertThat(trace.elapsedMillis()).isGreaterThanOrEqualTo(0L);
        verify(read).call("{}");
        verify(audit).record(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void shouldTrackFailedReadToolAndPreserveAuditFailure() {
        ToolCallback read = callback("getLeaveBalances");
        when(read.call("{}" )).thenThrow(new IllegalStateException("database unavailable"));
        AssistantAuditService audit = mock(AssistantAuditService.class);
        AssistantRequestTrace trace = new AssistantRequestTrace();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication(RbacPermissions.LEAVE_APPLICATION_READ), user(), new ObjectMapper(),
                new ArrayList<>(), new ArrayList<>(), "c-failed", mock(AssistantConfirmationService.class), audit, trace);

        assertThatThrownBy(() -> adapted[0].call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
        assertThat(trace.toolCallCount()).isEqualTo(1);
        assertThat(trace.lastStartedTool()).isEqualTo("getLeaveBalances");
        assertThat(trace.lastCompletedTool()).isEqualTo("getLeaveBalances");
        verify(audit).record(anyString(), anyString(), anyString(), anyString(), anyString(), any(),
                org.mockito.ArgumentMatchers.eq("FAILED"), org.mockito.ArgumentMatchers.eq("IllegalStateException"));
    }

    @Test
    void shouldKeepAllowedPlainTextReadResultsStructuredWithoutFailing() {
        ToolCallback read = callback("getLeaveApplicationById");
        when(read.call("{}" )).thenReturn("not-json");
        List<AssistantDtos.StructuredResult> results = new ArrayList<>();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication(RbacPermissions.LEAVE_APPLICATION_READ), user(), new ObjectMapper(),
                new ArrayList<>(), results, "c1", mock(AssistantConfirmationService.class), mock(AssistantAuditService.class));

        adapted[0].call("{}");
        assertThat(results).singleElement().satisfies(result -> assertThat(result.data()).isEqualTo("not-json"));
    }

    @Test
    void shouldNotEchoSensitiveUserReadResultsIntoStructuredBrowserData() {
        ToolCallback read = callback("getAllUsers");
        when(read.call("{}" )).thenReturn("[{\"loginName\":\"dennis\",\"password\":\"secret\"}]");
        List<AssistantDtos.StructuredResult> results = new ArrayList<>();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication(RbacPermissions.USER_READ), user(), new ObjectMapper(),
                new ArrayList<>(), results, "c1", mock(AssistantConfirmationService.class), mock(AssistantAuditService.class));

        assertThat(adapted[0].call("{}")).contains("dennis");
        assertThat(results).isEmpty();
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
