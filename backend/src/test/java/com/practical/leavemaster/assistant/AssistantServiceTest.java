package com.practical.leavemaster.assistant;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
import static org.mockito.Mockito.when;

class AssistantServiceTest {
    private ObjectProvider<ChatModel> chatModelProvider;
    private ChatModel chatModel;
    private ToolCallbackProvider toolProvider;
    private AppUserRepository userRepository;
    private AssistantService service;
    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        chatModelProvider = mock(ObjectProvider.class);
        chatModel = mock(ChatModel.class);
        toolProvider = mock(ToolCallbackProvider.class);
        userRepository = mock(AppUserRepository.class);
        service = new AssistantService(chatModelProvider, toolProvider, userRepository, new ObjectMapper(),
                mock(AssistantConfirmationService.class), mock(AssistantAuditService.class),
                mock(AssistantRateLimitService.class), mock(AssistantProviderGuard.class));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", "gemini");
        ReflectionTestUtils.setField(service, "model", "gemini-3.6-flash");
        ReflectionTestUtils.setField(service, "openAiApiKey", "");
        ReflectionTestUtils.setField(service, "geminiApiKey", "gem-secret-value");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);

        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(userRepository.findById("dennis")).thenReturn(Optional.of(AppUser.builder()
                .loginName("dennis").staffId("S1").tenantId("T1").active(true).build()));
        ToolCallback tenantReadCallback = callback("getAllTenants");
        when(toolProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{tenantReadCallback});

        serviceLogger = (Logger) LoggerFactory.getLogger(AssistantService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
        logAppender.stop();
        service.shutdownProviderExecutor();
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
    void shouldFailSafelyWhenProviderFailsAndLogDiagnostics() {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("Gemini rejected configured credential gem-secret-value"));

        assertThatThrownBy(() -> service.chat(
                new AssistantDtos.ChatRequest("Hello", "conversation-provider-failure"),
                authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(AssistantProviderException.class)
                .hasMessageContaining("could not complete");

        String logs = formattedLogs();
        assertThat(logs)
                .contains("Ask LeaveMaestro provider request failed")
                .contains("provider=gemini")
                .contains("model=gemini-3.6-flash")
                .contains("conversationId=conversation-provider-failure")
                .contains("exceptionType=java.lang.RuntimeException")
                .contains("rootCauseType=java.lang.RuntimeException")
                .contains("[REDACTED]")
                .doesNotContain("gem-secret-value");
    }

    @Test
    void shouldLogTimeoutWithProviderContext() {
        ReflectionTestUtils.setField(service, "timeoutSeconds", 0L);
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Thread.sleep(5_000);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("late response"))));
        });

        assertThatThrownBy(() -> service.chat(
                new AssistantDtos.ChatRequest("Hello", "conversation-timeout"),
                authentication(RbacPermissions.TENANT_READ)))
                .isInstanceOf(AssistantProviderException.class)
                .hasMessageContaining("timed out");

        assertThat(formattedLogs())
                .contains("Ask LeaveMaestro provider request timed out")
                .contains("provider=gemini")
                .contains("model=gemini-3.6-flash")
                .contains("conversationId=conversation-timeout")
                .contains("timeoutSeconds=0");
    }

    @Test
    void shouldRedactCredentialLikeValuesFromProviderMessages() {
        RuntimeException failure = new RuntimeException(
                "authorization=Bearer abc.def api_key=another-key token=token-value secret=secret-value gem-secret-value");

        assertThat(service.safeProviderMessage(failure))
                .contains("[REDACTED]")
                .doesNotContain("abc.def")
                .doesNotContain("another-key")
                .doesNotContain("token-value")
                .doesNotContain("secret-value")
                .doesNotContain("gem-secret-value");
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

    private String formattedLogs() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
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
