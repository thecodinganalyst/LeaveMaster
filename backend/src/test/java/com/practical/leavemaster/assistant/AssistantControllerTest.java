package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class AssistantControllerTest {

    private AssistantService service;
    private AssistantConfirmationService confirmationService;
    private AssistantController controller;
    private AssistantQualityService qualityService;
    private com.practical.leavemaster.user.AppUserRepository appUserRepository;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        service = mock(AssistantService.class);
        confirmationService = mock(AssistantConfirmationService.class);
        qualityService = mock(AssistantQualityService.class);
        appUserRepository = mock(com.practical.leavemaster.user.AppUserRepository.class);
        controller = new AssistantController(service, confirmationService, qualityService, appUserRepository);
        authentication = new UsernamePasswordAuthenticationToken("dennis", "n/a", List.of());
    }

    @Test
    void shouldReturnAssistantResponse() {
        var request = new AssistantDtos.ChatRequest("Hello", "c1");
        var expected = new AssistantDtos.ChatResponse("c1", "Hi", List.of(), List.of());
        when(service.chat(request, authentication)).thenReturn(expected);
        assertThat(controller.chat(request, authentication)).isSameAs(expected);
    }

    @Test
    void shouldConfirmPendingAction() {
        var request = new AssistantDtos.ConfirmationRequest("token-1");
        var expected = new AssistantDtos.ConfirmationResponse("applyForLeave", "EXECUTED", "ok", false);
        when(confirmationService.confirm("token-1", authentication)).thenReturn(expected);
        assertThat(controller.confirm(request, authentication)).isSameAs(expected);
    }


    @Test
    void shouldRecordFeedbackForAuthenticatedUsers() {
        var user = com.practical.leavemaster.user.AppUser.builder().loginName("dennis").tenantId("DEMO").build();
        when(appUserRepository.findById("dennis")).thenReturn(java.util.Optional.of(user));
        var result = controller.feedback(new AssistantController.FeedbackRequest("corr-1", 1, "helpful"), authentication);
        assertThat(result).containsEntry("status", "RECORDED");
        verify(qualityService).recordFeedback("corr-1", "DEMO", authentication, 1, "helpful");
    }

    @Test
    void shouldRejectInvalidFeedbackAndMissingAuthenticatedUser() {
        assertThatThrownBy(() -> controller.feedback(new AssistantController.FeedbackRequest(" ", 1, null), authentication))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("correlationId");
        when(appUserRepository.findById("dennis")).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> controller.feedback(new AssistantController.FeedbackRequest("corr", 1, null), authentication))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Authenticated user");
    }

    @Test
    void shouldReturnTenantScopedQualitySummary() {
        var user = com.practical.leavemaster.user.AppUser.builder().loginName("dennis").tenantId("DEMO").build();
        var expected = new AssistantQualityService.QualitySummary(3, 2, 1, 120.0, java.util.Map.of("TOOL_FAILURE", 1L));
        when(appUserRepository.findById("dennis")).thenReturn(java.util.Optional.of(user));
        when(qualityService.summary("DEMO")).thenReturn(expected);
        assertThat(controller.quality(authentication)).isEqualTo(expected);
    }

    @Test
    void shouldMapValidationProviderAndRateErrors() {
        var bad = controller.badRequest(new IllegalArgumentException("message is required"));
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody()).containsEntry("error", "message is required");

        var rate = controller.rateLimited(new AssistantRateLimitException("slow down"));
        assertThat(rate.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        var unavailable = controller.unavailable(new AssistantUnavailableException("disabled"));
        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        var provider = controller.providerFailure(new AssistantProviderException("provider failed", new RuntimeException()));
        assertThat(provider.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(provider.getBody()).containsOnlyKeys("error");
    }

    @Test
    void shouldExposeConversationIdForProviderFailureTroubleshooting() {
        var provider = controller.providerFailure(new AssistantProviderException(
                "The AI provider timed out", "conversation-timeout", new RuntimeException()));

        assertThat(provider.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(provider.getBody())
                .containsEntry("error", "The AI provider timed out")
                .containsEntry("conversationId", "conversation-timeout");
    }

    @Test
    void shouldMapToolFailureWithoutExposingInternalCause() {
        var tool = controller.toolFailure(new AssistantToolExecutionException(
                "LeaveMaster could not complete an assistant data lookup",
                "conversation-tool-failure",
                "getLeaveBalances",
                new IllegalStateException("Cannot lazily initialize secret internal detail")));

        assertThat(tool.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(tool.getBody())
                .containsEntry("error", "LeaveMaster could not complete an assistant data lookup")
                .containsEntry("conversationId", "conversation-tool-failure")
                .doesNotContainValue("Cannot lazily initialize secret internal detail")
                .doesNotContainKey("tool");
    }
}
