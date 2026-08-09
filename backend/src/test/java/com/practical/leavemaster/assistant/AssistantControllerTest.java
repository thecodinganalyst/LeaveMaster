package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantControllerTest {

    private AssistantService service;
    private AssistantConfirmationService confirmationService;
    private AssistantController controller;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        service = mock(AssistantService.class);
        confirmationService = mock(AssistantConfirmationService.class);
        controller = new AssistantController(service, confirmationService);
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
    }
}
