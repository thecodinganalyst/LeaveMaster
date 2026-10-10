package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationTenantControllerTest {
    @Mock DemoTenantSeedService seedService;
    EvaluationTenantController controller;

    @BeforeEach
    void setup() {
        controller = new EvaluationTenantController(seedService);
        ReflectionTestUtils.setField(controller, "enabled", true);
        ReflectionTestUtils.setField(controller, "resetToken", "private-reset-token");
    }

    @Test
    void refusesMissingAndIncorrectTokensWithoutResetting() {
        assertThat(controller.reset(null).getStatusCode().value()).isEqualTo(403);
        assertThat(controller.reset("incorrect").getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(seedService);
    }

    @Test
    void refusesWhenDisabled() {
        ReflectionTestUtils.setField(controller, "enabled", false);
        assertThat(controller.reset("private-reset-token").getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(seedService);
    }

    @Test
    void refusesUnconfiguredToken() {
        ReflectionTestUtils.setField(controller, "resetToken", "");
        assertThat(controller.reset("").getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(seedService);
    }

    @Test
    void authorizedResetReturnsEvaluationIdentity() {
        when(seedService.resetEvaluationTenant()).thenReturn(new DemoTenantSeedService.DemoSeedResult(
                "EVALUATION", TenantType.EVALUATION, 4, 4, 4, LocalDate.of(2026, 10, 10)));
        var response = controller.reset("private-reset-token");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().tenantType()).isEqualTo(TenantType.EVALUATION);
        verify(seedService).resetEvaluationTenant();
    }
}
