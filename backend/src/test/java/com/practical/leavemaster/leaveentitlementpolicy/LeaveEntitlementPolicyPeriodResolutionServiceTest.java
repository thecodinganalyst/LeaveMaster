package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementPolicyPeriodResolutionServiceTest {
    private static final String SOURCE_LEAVE_TYPE_ID = "sg-annual";

    @Mock private StaffRepository staffRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository ruleRepository;
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private LeaveEntitlementPolicyResolutionService service;

    @Test
    void resolvesWhenServiceThresholdIsReachedLaterInPeriod() {
        Staff staff = staff(LocalDate.of(2026, 8, 3));
        LeaveEntitlementPolicy policy = templatePolicy("p1", 10, null, null);
        setupTemplates(policy);
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1"))
                .thenReturn(List.of(serviceMonthsRule("r1", "p1", 3)));

        PolicyPeriodResolutionResult result = service.resolveTemplateInPeriod(
                staff, SOURCE_LEAVE_TYPE_ID, LocalDate.of(2026, 8, 22), LocalDate.of(2026, 12, 31));

        assertThat(result.templatesFound()).isTrue();
        assertThat(result.matchedDate()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(result.resolution().selectedPolicyId()).isEqualTo("p1");
    }

    @Test
    void doesNotResolveWhenServiceThresholdFallsAfterPeriodEnd() {
        Staff staff = staff(LocalDate.of(2026, 8, 3));
        LeaveEntitlementPolicy policy = templatePolicy("p1", 10, null, null);
        setupTemplates(policy);
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1"))
                .thenReturn(List.of(serviceMonthsRule("r1", "p1", 3)));

        PolicyPeriodResolutionResult result = service.resolveTemplateInPeriod(
                staff, SOURCE_LEAVE_TYPE_ID, LocalDate.of(2026, 8, 22), LocalDate.of(2026, 10, 31));

        assertThat(result.templatesFound()).isTrue();
        assertThat(result.matchedDate()).isNull();
        assertThat(result.resolution().selectedPolicyId()).isNull();
    }

    @Test
    void respectsFuturePolicyEffectiveDateWithinPeriod() {
        Staff staff = staff(LocalDate.of(2025, 1, 1));
        LocalDate effectiveFrom = LocalDate.of(2026, 10, 15);
        LeaveEntitlementPolicy policy = templatePolicy("p1", 10, effectiveFrom, null);
        setupTemplates(policy);
        when(ruleRepository.findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc("p1")).thenReturn(List.of());

        PolicyPeriodResolutionResult result = service.resolveTemplateInPeriod(
                staff, SOURCE_LEAVE_TYPE_ID, LocalDate.of(2026, 8, 22), LocalDate.of(2026, 12, 31));

        assertThat(result.matchedDate()).isEqualTo(effectiveFrom);
        assertThat(result.resolution().selectedPolicyId()).isEqualTo("p1");
    }

    @Test
    void reportsNoTemplatesAndRejectsInvalidPeriod() {
        Staff staff = staff(LocalDate.of(2026, 8, 3));
        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of());
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(singapore()));

        PolicyPeriodResolutionResult result = service.resolveTemplateInPeriod(
                staff, SOURCE_LEAVE_TYPE_ID, LocalDate.of(2026, 8, 22), LocalDate.of(2026, 12, 31));
        assertThat(result.templatesFound()).isFalse();
        assertThat(result.matchedDate()).isNull();

        assertThatThrownBy(() -> service.resolveTemplateInPeriod(
                staff, SOURCE_LEAVE_TYPE_ID, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid policy resolution period");
    }

    private void setupTemplates(LeaveEntitlementPolicy... policies) {
        when(policyRepository.findAllByScopeAndJurisdictionIdAndTenantIdIsNullAndActiveTrue(
                ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(policies));
        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(singapore()));
    }

    private Staff staff(LocalDate joinDate) {
        return Staff.builder()
                .id("__preview__")
                .name("Preview")
                .tenantId("tenant-a")
                .joinDate(joinDate)
                .jurisdictionId("SG")
                .build();
    }

    private LeaveEntitlementPolicy templatePolicy(String id, int priority, LocalDate from, LocalDate to) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .tenantId(null)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId(SOURCE_LEAVE_TYPE_ID)
                .name(id)
                .active(true)
                .priority(priority)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(BigDecimal.TEN)
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.MONTHS)
                .effectiveFrom(from)
                .effectiveTo(to)
                .build();
    }

    private LeaveEntitlementPolicyEligibilityRule serviceMonthsRule(
            String id, String policyId, long months) {
        return LeaveEntitlementPolicyEligibilityRule.builder()
                .id(id)
                .policyId(policyId)
                .criterionType(EligibilityCriterionType.SERVICE_MONTHS)
                .operator(EligibilityOperator.GREATER_THAN_OR_EQUAL)
                .value(Long.toString(months))
                .active(true)
                .sortOrder(1)
                .build();
    }

    private Jurisdiction singapore() {
        return Jurisdiction.builder()
                .id("SG")
                .code("SG")
                .name("Singapore")
                .countryCode("SG")
                .jurisdictionType(JurisdictionType.COUNTRY)
                .active(true)
                .build();
    }
}
