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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantToolAdapterTest {

    @Test
    void shouldExecuteAuthorizedReadToolNormally() {
        ToolCallback read = callback("getAllTenants");
        when(read.call("{}" )).thenReturn("[]");
        var authentication = authentication(RbacPermissions.TENANT_READ);

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{read}, authentication, user(), new ObjectMapper(), new ArrayList<>());

        assertThat(adapted).hasSize(1);
        assertThat(adapted[0].call("{}")).isEqualTo("[]");
        verify(read).call("{}");
    }

    @Test
    void shouldTurnAuthorizedWriteIntoPendingActionWithoutExecutingIt() {
        ToolCallback write = callback("createTenant");
        var authentication = authentication(RbacPermissions.TENANT_WRITE);
        List<AssistantDtos.PendingAction> pending = new ArrayList<>();

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{write}, authentication, user(), new ObjectMapper(), pending);

        String result = adapted[0].call("{\"id\":\"T1\",\"name\":\"Tenant One\"}");

        assertThat(result).contains("confirmation");
        assertThat(pending).singleElement().satisfies(action -> {
            assertThat(action.toolName()).isEqualTo("createTenant");
            assertThat(action.requiredAuthority()).isEqualTo(RbacPermissions.TENANT_WRITE);
            assertThat(action.actorLoginName()).isEqualTo("dennis");
            assertThat(action.tenantId()).isEqualTo("tenant-1");
            assertThat(action.arguments()).containsEntry("id", "T1");
        });
        verify(write, never()).call("{\"id\":\"T1\",\"name\":\"Tenant One\"}");
    }

    @Test
    void shouldNotExposeToolWithoutRequiredAuthority() {
        ToolCallback write = callback("deleteTenant");

        ToolCallback[] adapted = AssistantToolAdapter.forUser(
                new ToolCallback[]{write}, authentication(RbacPermissions.TENANT_READ), user(), new ObjectMapper(), new ArrayList<>());

        assertThat(adapted).isEmpty();
    }

    private ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\"}")
                .build());
        when(callback.getToolMetadata()).thenReturn(ToolMetadata.builder().build());
        return callback;
    }

    private UsernamePasswordAuthenticationToken authentication(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "dennis", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }

    private AppUser user() {
        return AppUser.builder()
                .loginName("dennis")
                .staffId("S1")
                .tenantId("tenant-1")
                .active(true)
                .build();
    }
}
