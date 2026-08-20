package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionType;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyResolutionServiceTest {
    @Mock private StaffRepository staffRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveEntitlementPolicyResolutionService service;

    private final LocalDate date = LocalDate.of(2026, 8, 14);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void selectsHighestPriorityMatchingPolicyWithAndRules() {
        Staff staff = staff("staff-1", "tenant-a", LocalDate.of(2025, 1, 1), "SG");
        LeaveEntitlementPolicy standard = policy("p1", 10, LocalDate.of(2026, 1, 1), null);
        LeaveEntitlementPolicy senior = policy("p2", 20, LocalDate.of(2026, 1, 1), null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "annual"))
                .thenReturn(List.of(standard, senior));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1"))
                .thenReturn(List.of(rule("r1", "p1", EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN_OR_EQUAL, "6")));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p2"))
                .thenReturn(List.of(
                        rule("r2", "p2", EligibilityCriterionType.SERVICE_MONTHS, EligibilityOperator.GREATER_THAN_OR_EQUAL, "12"),
                        rule("r3", "p2", EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "SG")));
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(jurisdiction("SG", "Singapore", null, true)));

        PolicyResolutionResult result = service.resolve("staff-1", "annual", date);

        assertThat(result.selectedPolicyId()).isEqualTo("p2");
        assertThat(result.ambiguous()).isFalse();
        assertThat(result.consideredPolicies()).hasSize(2).allMatch(PolicyResolutionResult.PolicyEvaluation::matched);
    }

    @Test
    void supportsAllNumericOperatorsAndSetOperators() {
        Staff staff = staff("staff-1", "tenant-a", LocalDate.of(2025, 8, 14), null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));

        for (EligibilityOperator operator : EligibilityOperator.values()) {
            LeaveEntitlementPolicy policy = policy("p-" + operator, 1, LocalDate.of(2026, 1, 1), null);
            when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", operator.name()))
                    .thenReturn(List.of(policy));
            String value = switch (operator) {
                case EQUALS, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL -> "12";
                case NOT_EQUALS, LESS_THAN -> "13";
                case GREATER_THAN -> "11";
                case IN -> "6,12,24";
                case NOT_IN -> "6,24";
            };
            when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(policy.getId()))
                    .thenReturn(List.of(rule("r-" + operator, policy.getId(), EligibilityCriterionType.SERVICE_MONTHS, operator, value)));

            assertThat(service.resolve("staff-1", operator.name(), date).selectedPolicyId()).isEqualTo(policy.getId());
        }
    }

    @Test
    void jurisdictionRuleMatchesAssignedJurisdictionAndParentHierarchy() {
        Staff staff = staff("staff-1", "tenant-a", LocalDate.of(2025, 1, 1), "AU-NSW");
        LeaveEntitlementPolicy policy = policy("p1", 10, LocalDate.of(2026, 1, 1), null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "annual")).thenReturn(List.of(policy));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1"))
                .thenReturn(List.of(rule("r1", "p1", EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.IN, "AU,AU-NSW")));
        when(jurisdictionRepository.findById("AU-NSW")).thenReturn(Optional.of(
                Jurisdiction.builder().id("AU-NSW").code("AU-NSW").name("New South Wales")
                        .countryCode("AU").subdivisionCode("NSW").parentId("AU")
                        .jurisdictionType(JurisdictionType.STATE).active(true).build()));
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(jurisdiction("AU", "Australia", null, true)));

        PolicyResolutionResult result = service.resolve("staff-1", "annual", date);

        assertThat(result.selectedPolicyId()).isEqualTo("p1");
        assertThat(result.consideredPolicies().getFirst().rules().getFirst().matched()).isTrue();
    }

    @Test
    void inactiveJurisdictionDoesNotMatch() {
        Staff staff = staff("staff-1", "tenant-a", LocalDate.of(2025, 1, 1), "AU-VIC");
        LeaveEntitlementPolicy policy = policy("p1", 10, LocalDate.of(2026, 1, 1), null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "annual")).thenReturn(List.of(policy));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1"))
                .thenReturn(List.of(rule("r1", "p1", EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "AU-VIC")));
        when(jurisdictionRepository.findById("AU-VIC")).thenReturn(Optional.of(
                Jurisdiction.builder().id("AU-VIC").code("AU-VIC").name("Victoria").countryCode("AU")
                        .jurisdictionType(JurisdictionType.STATE).active(false).build()));

        PolicyResolutionResult result = service.resolve("staff-1", "annual", date);

        assertThat(result.selectedPolicyId()).isNull();
    }

    @Test
    void returnsNoMatchForFailedAndRuleAndForOutOfRangePolicy() {
        Staff staff = staff("staff-1", "tenant-a", LocalDate.of(2026, 1, 1), "SG");
        LeaveEntitlementPolicy failedRule = policy("p1", 10, LocalDate.of(2026, 1, 1), null);
        LeaveEntitlementPolicy expired = policy("p2", 20, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "annual")).thenReturn(List.of(failedRule, expired));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1"))
                .thenReturn(List.of(rule("r1", "p1", EligibilityCriterionType.JURISDICTION_CODE, EligibilityOperator.EQUALS, "AU")));
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(jurisdiction("SG", "Singapore", null, true)));

        PolicyResolutionResult result = service.resolve("staff-1", "annual", date);

        assertThat(result.selectedPolicyId()).isNull();
        assertThat(result.reason()).isEqualTo("No matching policy");
        assertThat(result.consideredPolicies()).extracting(PolicyResolutionResult.PolicyEvaluation::matched).containsOnly(false);
    }

    @Test
    void equalHighestPrioritiesAreReportedAsAmbiguous() {
        Staff staff = staff("staff-1", "tenant-a", LocalDate.of(2025, 1, 1), null);
        LeaveEntitlementPolicy first = policy("p1", 10, LocalDate.of(2026, 1, 1), null);
        LeaveEntitlementPolicy second = policy("p2", 10, LocalDate.of(2026, 1, 1), null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "annual")).thenReturn(List.of(first, second));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1")).thenReturn(List.of());
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p2")).thenReturn(List.of());

        PolicyResolutionResult result = service.resolve("staff-1", "annual", date);

        assertThat(result.ambiguous()).isTrue();
        assertThat(result.selectedPolicyId()).isNull();
    }

    @Test
    void tenantUserCannotResolveAnotherTenantButPlatformAdminCan() {
        Staff staff = staff("staff-1", "tenant-b", LocalDate.of(2025, 1, 1), null);
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        authenticate("hr", "tenant-a", Set.of());
        assertThatThrownBy(() -> service.resolve("staff-1", "annual", date)).isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();
        authenticate("platform", null, Set.of(AppRole.builder().id("PLATFORM_ADMIN").description("Platform").active(true).build()));
        when(policyRepository.findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-b", "annual")).thenReturn(List.of());
        assertThat(service.resolve("staff-1", "annual", date).reason()).isEqualTo("No matching policy");
    }

    private void authenticate(String login, String tenantId, Set<AppRole> roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login).active(true).tenantId(tenantId).roles(roles).build()));
    }

    private Staff staff(String id, String tenantId, LocalDate joinDate, String jurisdictionId) {
        return Staff.builder().id(id).name(id).tenantId(tenantId).joinDate(joinDate).jurisdictionId(jurisdictionId).build();
    }

    private LeaveEntitlementPolicy policy(String id, int priority, LocalDate from, LocalDate to) {
        return LeaveEntitlementPolicy.builder().id(id).tenantId("tenant-a").leaveTypeId("annual").name(id)
                .active(true).priority(priority).entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.TEN).accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.MONTHS).effectiveFrom(from).effectiveTo(to).build();
    }

    private LeaveEntitlementPolicyEligibilityRule rule(String id, String policyId, EligibilityCriterionType type, EligibilityOperator operator, String value) {
        return LeaveEntitlementPolicyEligibilityRule.builder().id(id).policyId(policyId).criterionType(type)
                .operator(operator).value(value).active(true).sortOrder(1).build();
    }

    private Jurisdiction jurisdiction(String code, String name, String parentId, boolean active) {
        return Jurisdiction.builder().id(code).code(code).name(name).countryCode(code.substring(0, 2))
                .parentId(parentId).jurisdictionType(JurisdictionType.COUNTRY).active(active).build();
    }
}
