package com.practical.leavemaster.leaveentitlementpolicy;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveEntitlementPolicyEligibilityControllerTest {

    @Test
    void nestedControllerDelegatesCrudAndResolution() {
        LeaveEntitlementPolicyEligibilityService eligibility = Mockito.mock(LeaveEntitlementPolicyEligibilityService.class);
        LeaveEntitlementPolicyResolutionService resolution = Mockito.mock(LeaveEntitlementPolicyResolutionService.class);
        LeaveEntitlementPolicyEligibilityController controller = new LeaveEntitlementPolicyEligibilityController(eligibility, resolution);
        LeaveEntitlementPolicyEligibilityRule rule = LeaveEntitlementPolicyEligibilityRule.builder().id("r1").policyId("p1").build();
        when(eligibility.findAll("p1")).thenReturn(List.of(rule));
        when(eligibility.create("p1", rule)).thenReturn(rule);
        when(eligibility.update("p1", "r1", rule)).thenReturn(rule);
        PolicyResolutionResult result = new PolicyResolutionResult("s1", "annual", "p1", false, "selected", List.of());
        LocalDate date = LocalDate.of(2026, 8, 14);
        when(resolution.resolve("s1", "annual", date)).thenReturn(result);

        assertThat(controller.getRules("p1")).containsExactly(rule);
        assertThat(controller.createRule("p1", rule).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateRule("p1", "r1", rule)).isSameAs(rule);
        assertThat(controller.deleteRule("p1", "r1").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.resolve("s1", "annual", date)).isSameAs(result);
        verify(eligibility).delete("p1", "r1");
    }

    @Test
    void flatControllerDelegatesCrud() {
        LeaveEntitlementPolicyEligibilityService eligibility = Mockito.mock(LeaveEntitlementPolicyEligibilityService.class);
        LeaveEntitlementPolicyEligibilityResourceController controller = new LeaveEntitlementPolicyEligibilityResourceController(eligibility);
        LeaveEntitlementPolicyEligibilityRule rule = LeaveEntitlementPolicyEligibilityRule.builder().id("r1").policyId("p1").build();
        when(eligibility.findAllAccessible()).thenReturn(List.of(rule));
        when(eligibility.findById("r1")).thenReturn(Optional.of(rule));
        when(eligibility.findById("missing")).thenReturn(Optional.empty());
        when(eligibility.create(rule)).thenReturn(rule);
        when(eligibility.update("r1", rule)).thenReturn(rule);

        assertThat(controller.getAll()).containsExactly(rule);
        assertThat(controller.getById("r1").getBody()).isSameAs(rule);
        assertThat(controller.getById("missing").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.create(rule).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.update("r1", rule)).isSameAs(rule);
        assertThat(controller.delete("r1").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(eligibility).delete("r1");
    }
}
