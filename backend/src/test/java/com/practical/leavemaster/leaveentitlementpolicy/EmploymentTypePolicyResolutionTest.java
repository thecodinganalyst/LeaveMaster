package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.staff.EmploymentType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploymentTypePolicyResolutionTest {
    @Mock private StaffRepository staffRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private DependantEligibilityMatcher dependantEligibilityMatcher;
    @InjectMocks private LeaveEntitlementPolicyResolutionService service;

    private final LocalDate date = LocalDate.of(2026, 8, 26);

    @Test
    void resolvesEmploymentTypeWithAllSupportedSetOperators() {
        assertMatch(EmploymentType.FULL_TIME, EligibilityOperator.EQUALS, "FULL_TIME", true);
        assertMatch(EmploymentType.FULL_TIME, EligibilityOperator.NOT_EQUALS, "PART_TIME", true);
        assertMatch(EmploymentType.FULL_TIME, EligibilityOperator.IN, "PART_TIME,FULL_TIME", true);
        assertMatch(EmploymentType.FULL_TIME, EligibilityOperator.NOT_IN, "CASUAL,INTERN", true);
        assertMatch(EmploymentType.FULL_TIME, EligibilityOperator.EQUALS, "PART_TIME", false);
        assertMatch(EmploymentType.FULL_TIME, EligibilityOperator.IN, "CASUAL,CONTRACT", false);
    }

    @Test
    void handlesLegacyNullEmploymentTypeAsNoActualValue() {
        assertMatch(null, EligibilityOperator.EQUALS, "FULL_TIME", false);
        assertMatch(null, EligibilityOperator.IN, "FULL_TIME,PART_TIME", false);
        assertMatch(null, EligibilityOperator.NOT_EQUALS, "FULL_TIME", true);
        assertMatch(null, EligibilityOperator.NOT_IN, "FULL_TIME,PART_TIME", true);
    }

    @Test
    void combinesEmploymentTypeWithExistingRulesUsingAndSemantics() {
        Staff staff = staff(EmploymentType.PART_TIME);
        LeaveEntitlementPolicy policy = policy("combined");
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "annual"))
                .thenReturn(List.of(policy));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("combined")).thenReturn(List.of(
                rule("employment", EligibilityCriterionType.EMPLOYMENT_TYPE, EligibilityOperator.EQUALS, "PART_TIME"),
                rule("service", EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN_OR_EQUAL, "6")));

        assertThat(service.resolve("staff-1", "annual", date).selectedPolicyId()).isEqualTo("combined");
    }

    private void assertMatch(
            EmploymentType employmentType,
            EligibilityOperator operator,
            String value,
            boolean expectedMatch) {
        String policyId = "p-" + operator + "-" + value.replace(',', '-');
        Staff staff = staff(employmentType);
        LeaveEntitlementPolicy policy = policy(policyId);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", policyId))
                .thenReturn(List.of(policy));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(policyId))
                .thenReturn(List.of(rule("rule", EligibilityCriterionType.EMPLOYMENT_TYPE, operator, value)));

        PolicyResolutionResult result = service.resolve("staff-1", policyId, date);
        assertThat(result.selectedPolicyId() != null).isEqualTo(expectedMatch);
        assertThat(result.consideredPolicies().getFirst().rules().getFirst().matched()).isEqualTo(expectedMatch);
    }

    private Staff staff(EmploymentType employmentType) {
        return Staff.builder()
                .id("staff-1")
                .name("Staff")
                .tenantId("tenant-a")
                .joinDate(LocalDate.of(2025, 1, 1))
                .employmentType(employmentType)
                .build();
    }

    private LeaveEntitlementPolicy policy(String id) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .tenantId("tenant-a")
                .leaveTypeId("annual")
                .name(id)
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.TEN)
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.MONTHS)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
    }

    private LeaveEntitlementPolicyEligibilityRule rule(
            String id,
            EligibilityCriterionType criterion,
            EligibilityOperator operator,
            String value) {
        return LeaveEntitlementPolicyEligibilityRule.builder()
                .id(id)
                .policyId("annual")
                .criterionType(criterion)
                .operator(operator)
                .value(value)
                .active(true)
                .sortOrder(10)
                .build();
    }
}
