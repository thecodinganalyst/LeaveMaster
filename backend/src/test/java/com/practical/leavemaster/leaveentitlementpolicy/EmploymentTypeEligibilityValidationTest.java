package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploymentTypeEligibilityValidationTest {
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private LeaveEntitlementPolicyService policyService;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @InjectMocks private LeaveEntitlementPolicyEligibilityService service;

    @Test
    void acceptsSupportedEmploymentTypeSetOperatorsAndValues() {
        when(policyService.findById("p1")).thenReturn(Optional.of(policy()));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create("p1", rule(EligibilityOperator.EQUALS, "FULL_TIME")).getValue())
                .isEqualTo("FULL_TIME");
        assertThat(service.create("p1", rule(EligibilityOperator.NOT_EQUALS, "CASUAL")).getValue())
                .isEqualTo("CASUAL");
        assertThat(service.create("p1", rule(EligibilityOperator.IN, "FULL_TIME, PART_TIME")).getValue())
                .isEqualTo("FULL_TIME, PART_TIME");
        assertThat(service.create("p1", rule(EligibilityOperator.NOT_IN, "CONTRACT,INTERN")).getValue())
                .isEqualTo("CONTRACT,INTERN");
    }

    @Test
    void rejectsInvalidEmploymentTypeRules() {
        when(policyService.findById("p1")).thenReturn(Optional.of(policy()));

        assertThatThrownBy(() -> service.create("p1", rule(EligibilityOperator.GREATER_THAN, "FULL_TIME")))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("supports only EQUALS, NOT_EQUALS, IN and NOT_IN");
        assertThatThrownBy(() -> service.create("p1", rule(EligibilityOperator.EQUALS, "PERMANENT")))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("Unknown employment type: PERMANENT");
        assertThatThrownBy(() -> service.create("p1", rule(EligibilityOperator.EQUALS, "FULL_TIME,PART_TIME")))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("require one value");
        assertThatThrownBy(() -> service.create("p1", rule(EligibilityOperator.IN, ",,")))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("requires at least one value");
    }

    private LeaveEntitlementPolicy policy() {
        return LeaveEntitlementPolicy.builder()
                .id("p1")
                .tenantId("tenant-a")
                .scope(ConfigurationScope.TENANT)
                .leaveTypeId("annual")
                .name("Annual")
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.TEN)
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.MONTHS)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
    }

    private LeaveEntitlementPolicyEligibilityRule rule(EligibilityOperator operator, String value) {
        return LeaveEntitlementPolicyEligibilityRule.builder()
                .criterionType(EligibilityCriterionType.EMPLOYMENT_TYPE)
                .operator(operator)
                .value(value)
                .active(true)
                .sortOrder(10)
                .build();
    }
}
