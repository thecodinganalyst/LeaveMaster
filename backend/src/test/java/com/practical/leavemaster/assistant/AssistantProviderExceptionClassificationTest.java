package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantProviderExceptionClassificationTest {

    private ChatModel chatModel;
    private AssistantProviderGuard providerGuard;
    private AssistantService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
        chatModel = mock(ChatModel.class);
        ToolCallbackProvider toolProvider = mock(ToolCallbackProvider.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        providerGuard = mock(AssistantProviderGuard.class);
        service = new AssistantService(chatModelProvider, toolProvider, userRepository, new ObjectMapper(),
                mock(AssistantConfirmationService.class), mock(AssistantAuditService.class),
                mock(AssistantRateLimitService.class), providerGuard);

        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", "gemini");
        ReflectionTestUtils.setField(service, "model", "gemini-3.6-flash");
        ReflectionTestUtils.setField(service, "openAiApiKey", "");
        ReflectionTestUtils.setField(service, "geminiApiKey", "");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);

        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(userRepository.findById("dennis")).thenReturn(Optional.of(AppUser.builder()
                .loginName("dennis").staffId("S1").tenantId("T1").active(true).build()));
        ToolCallback tenantReadCallback = callback("getAllTenants");
        when(toolProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{tenantReadCallback});
    }

    @AfterEach
    void tearDown() {
        service.shutdownProviderExecutor();
    }

    @Test
    void shouldClassifyProviderIllegalArgumentExceptionAsProviderFailure() {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new IllegalArgumentException("Google's Structured Output schema doesn't support $defs property"));

        assertThatThrownBy(() -> service.chat(
                new AssistantDtos.ChatRequest("Why is the staff given 5.52 days of annual leave?", "conversation-324"),
                authentication()))
                .isInstanceOfSatisfying(AssistantProviderException.class, exception ->
                        assertThat(exception.getConversationId()).isEqualTo("conversation-324"))
                .hasMessageContaining("could not complete");

        verify(providerGuard).failure();
    }

    @Test
    void shouldKeepRequestValidationIllegalArgumentExceptionAsClientInputFailure() {
        assertThatThrownBy(() -> service.chat(new AssistantDtos.ChatRequest(" ", null), authentication()))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(AssistantProviderException.class)
                .hasMessage("message is required");
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken("dennis", "n/a",
                List.of(new SimpleGrantedAuthority(RbacPermissions.TENANT_READ)));
    }

    private ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\"}")
                .build());
        return callback;
    }
}
