package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantServiceTest {

    private ObjectProvider<ChatModel> chatModelProvider;
    private ChatModel chatModel;
    private ToolCallbackProvider toolProvider;
    private AppUserRepository userRepository;
    private AssistantService service;

    @BeforeEach
    void setUp() {
        chatModelProvider = mock(ObjectProvider.class);
        chatModel = mock(ChatModel.class);
        toolProvider = mock(ToolCallbackProvider.class);
        userRepository = mock(AppUserRepository.class);
        service = new AssistantService(chatModelProvider, toolProvider, userRepository, new ObjectMapper());
        ReflectionTestUtils.setField(service, "enabled", true);

        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(userRepository.findById("dennis")).thenReturn(Optional.of(AppUser.builder()
                .loginName("dennis").staffId("S1").tenantId("T1").active(true).build()));

        ToolCallback tenantReadCallback = callback("getAllTenants");
        when(toolProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{tenantReadCallback});
    }

    @Test
    void shouldReturnModelResponseWithConversationId() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("You have access.")))));

        var result = service.chat(new AssistantDtos.ChatRequest("What can I see?", null), authentication(RbacPermissions.TENANT_READ));

        assertThat(result.message()).isEqualTo("You have access.");
        assertThat(result.conversationId()).isNotBlank();
        assertThat(result.pendingActions()).isEmpty();
    }

    @Test
    void shouldPreserveSuppliedConversationId() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Hello")))));

        var result = service.chat(new AssistantDtos.ChatRequest("Hello", "conversation-1"), authentication(RbacPermissions.TENANT_READ));

        assertThat(result.conversationId()).isEqualTo("conversation-1");
    }

    @Test
    void shouldFailSafelyWhenProviderFails() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("provider down"));

        assertThatThrownBy(() -> service.chat(new AssistantDtos.ChatRequest("Hello", null), authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(AssistantProviderException.class)
                .hasMessageContaining("could not complete");
    }

    @Test
    void shouldRejectBlankMessageAndDisabledProvider() {
        assertThatThrownBy(() -> service.chat(new AssistantDtos.ChatRequest(" ", null), authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(IllegalArgumentException.class);

        ReflectionTestUtils.setField(service, "enabled", false);
        assertThatThrownBy(() -> service.chat(new AssistantDtos.ChatRequest("Hello", null), authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void shouldFailWhenModelOrUserIsUnavailable() {
        when(chatModelProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.chat(new AssistantDtos.ChatRequest("Hello", null), authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(AssistantUnavailableException.class);

        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(userRepository.findById("dennis")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.chat(new AssistantDtos.ChatRequest("Hello", null), authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    private UsernamePasswordAuthenticationToken authentication(String authority) {
        return new UsernamePasswordAuthenticationToken("dennis", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }

    private ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name).description(name).inputSchema("{\"type\":\"object\"}").build());
        return callback;
    }
}
