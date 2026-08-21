package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyTemplateResolutionTest {

    @Mock private StaffRepository staffRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveEntitlementPolicyResolutionService service;

    private final LocalDate date = LocalDate.of(2026, 8, 21);
    private Staff staff;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(AppUser.builder()
                .loginName("hr").active(true).tenantId("tenant-a").build()));
        staff = Staff.builder()
                .id("__preview__")
                .name("Preview")
                .tenantId("tenant-a")
                .jurisdictionId("SG")
                .joinDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesOnlyTenantlessPlatformTemplatesAndAppliesEligibility() {
        LeaveEntitlementPolicy eligible = template("template-eligible", "sg-annual", 20);
        LeaveEntitlementPolicy ineligible = template("template-ineligible", "sg-annual", 10);
        LeaveEntitlementPolicy otherType = template("template-sick", "sg-sick", 30);
        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "SG"))
                .thenReturn(List.of(eligible, ineligible, otherType));
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(jurisdiction("SG", null)));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("template-eligible"))
                .thenReturn(List.of(rule("eligible-rule", "template-eligible", EligibilityCriterionType.JURISDICTION_CODE,
                        EligibilityOperator.EQUALS, "SG")));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("template-ineligible"))
                .thenReturn(List.of(rule("ineligible-rule", "template-ineligible", EligibilityCriterionType.SERVICE_MONTHS,
                        EligibilityOperator.GREATER_THAN, "12")));

        PolicyResolutionResult result = service.resolveTemplate(staff, "sg-annual", date);

        assertThat(result.selectedPolicyId()).isEqualTo("template-eligible");
        assertThat(result.consideredPolicies()).hasSize(2);
        assertThat(result.consideredPolicies()).extracting(PolicyResolutionResult.PolicyEvaluation::matched)
                .containsExactly(true, false);
        verify(policyRepository, never()).findAllByTenantIdAndLeaveTypeIdAndActiveTrue("tenant-a", "sg-annual");
    }

    @Test
    void inheritsParentTemplateButLetsChildTemplateOverrideSameName() {
        Staff stateStaff = Staff.builder()
                .id("__preview__")
                .name("Preview")
                .tenantId("tenant-a")
                .jurisdictionId("AU-NSW")
                .joinDate(LocalDate.of(2025, 1, 1))
                .build();
        LeaveEntitlementPolicy child = template("child", "au-annual", 10);
        child.setName("Annual Leave");
        LeaveEntitlementPolicy parentSameName = template("parent-old", "au-annual", 50);
        parentSameName.setName("Annual Leave");
        LeaveEntitlementPolicy parentExtra = template("parent-extra", "au-annual", 20);
        parentExtra.setName("Long Service Annual Leave");

        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "AU-NSW")).thenReturn(List.of(child));
        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "AU")).thenReturn(List.of(parentSameName, parentExtra));
        when(jurisdictionRepository.findById("AU-NSW")).thenReturn(Optional.of(jurisdiction("AU-NSW", "AU")));
        when(jurisdictionRepository.findById("AU")).thenReturn(Optional.of(jurisdiction("AU", null)));
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("child")).thenReturn(List.of());
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("parent-extra")).thenReturn(List.of());

        PolicyResolutionResult result = service.resolveTemplate(stateStaff, "au-annual", date);

        assertThat(result.consideredPolicies()).extracting(PolicyResolutionResult.PolicyEvaluation::policyId)
                .containsExactly("child", "parent-extra");
        assertThat(result.selectedPolicyId()).isEqualTo("parent-extra");
    }

    @Test
    void returnsNoMatchWhenTemplateIsOutsideEffectiveRange() {
        LeaveEntitlementPolicy expired = template("expired", "sg-annual", 10);
        expired.setEffectiveTo(LocalDate.of(2025, 12, 31));
        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(expired));
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(jurisdiction("SG", null)));

        PolicyResolutionResult result = service.resolveTemplate(staff, "sg-annual", date);

        assertThat(result.selectedPolicyId()).isNull();
        assertThat(result.consideredPolicies().getFirst().effective()).isFalse();
    }

    private LeaveEntitlementPolicy template(String id, String sourceLeaveTypeId, int priority) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .tenantId(null)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId(sourceLeaveTypeId)
                .name(id)
                .active(true)
                .priority(priority)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.TEN)
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.NONE)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
    }

    private LeaveEntitlementPolicyEligibilityRule rule(
            String id, String policyId, EligibilityCriterionType type, EligibilityOperator operator, String value) {
        return LeaveEntitlementPolicyEligibilityRule.builder()
                .id(id)
                .policyId(policyId)
                .criterionType(type)
                .operator(operator)
                .value(value)
                .active(true)
                .sortOrder(1)
                .build();
    }

    private Jurisdiction jurisdiction(String id, String parentId) {
        return Jurisdiction.builder()
                .id(id)
                .code(id)
                .name(id)
                .countryCode(id.substring(0, 2))
                .parentId(parentId)
                .jurisdictionType(parentId == null ? JurisdictionType.COUNTRY : JurisdictionType.STATE)
                .active(true)
                .build();
    }
}
