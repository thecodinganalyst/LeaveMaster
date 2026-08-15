package com.practical.leavemaster.tenant;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.jurisdiction.Jurisdiction;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveType;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeRepository;
import com.practical.leavemaster.jurisdiction.JurisdictionLeaveTypeService;
import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarRepository;
import com.practical.leavemaster.leavecalendar.PublicHoliday;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityCriterionType;
import com.practical.leavemaster.leaveentitlementpolicy.EligibilityOperator;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyEligibilityRule;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantLeaveConfigurationProvisionServiceTest {
    @Mock private JurisdictionRepository jurisdictionRepository;
    @Mock private JurisdictionLeaveTypeService jurisdictionLeaveTypeService;
    @Mock private JurisdictionLeaveTypeRepository jurisdictionLeaveTypeRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyEligibilityRepository eligibilityRepository;
    @Mock private LeaveCalendarRepository leaveCalendarRepository;

    @InjectMocks private TenantLeaveConfigurationProvisionService service;

    @Test
    void shouldSeedLeaveTypesPoliciesEligibilityAndCalendars() {
        Tenant tenant = Tenant.builder().id("acme").jurisdictionId("SG").build();
        Jurisdiction sg = Jurisdiction.builder().id("SG").code("SG").name("Singapore").active(true).build();
        JurisdictionLeaveType annual = JurisdictionLeaveType.builder()
                .id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE").name("Annual Leave").active(true).build();
        LeaveEntitlementPolicy template = LeaveEntitlementPolicy.builder()
                .id("template-annual").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("SG")
                .jurisdictionLeaveTypeId(annual.getId()).name("Standard Annual Leave").active(true).priority(10)
                .entitlementUnit(EntitlementUnit.DAYS).entitlementAmount(new BigDecimal("14"))
                .accrualMethod(AccrualMethod.ANNUAL).prorationMethod(ProrationMethod.NONE)
                .carryForwardAllowed(false).effectiveFrom(LocalDate.of(2026, 1, 1)).build();
        LeaveEntitlementPolicyEligibilityRule rule = LeaveEntitlementPolicyEligibilityRule.builder()
                .id("rule-1").policyId(template.getId()).criterionType(EligibilityCriterionType.SERVICE_MONTHS)
                .operator(EligibilityOperator.GREATER_THAN_OR_EQUAL).value("0").active(true).sortOrder(1).build();
        LeaveCalendar calendar = LeaveCalendar.builder()
                .id("sg-2026").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("SG")
                .start(LocalDate.of(2026, 1, 1)).end(LocalDate.of(2026, 12, 31))
                .publicHolidays(List.of(PublicHoliday.builder().holidayDate(LocalDate.of(2026, 1, 1)).holidayName("New Year").build()))
                .build();

        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(sg));
        when(jurisdictionLeaveTypeService.resolveEffective("SG")).thenReturn(List.of(annual));
        when(leaveTypeRepository.findAllByTenantId("acme")).thenReturn(List.of());
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(template));
        when(jurisdictionLeaveTypeRepository.findById(annual.getId())).thenReturn(Optional.of(annual));
        when(policyRepository.existsByTenantIdAndSourceTemplateId("acme", template.getId())).thenReturn(false);
        when(policyRepository.save(any(LeaveEntitlementPolicy.class))).thenAnswer(invocation -> {
            LeaveEntitlementPolicy policy = invocation.getArgument(0);
            if (policy.getId() == null) policy.setId("tenant-policy");
            return policy;
        });
        when(eligibilityRepository.findAllByPolicyIdOrderBySortOrderAsc(template.getId())).thenReturn(List.of(rule));
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(calendar));
        when(leaveCalendarRepository.existsByTenantIdAndSourceTemplateId("acme", calendar.getId())).thenReturn(false);

        service.provision(tenant);

        verify(leaveTypeRepository).save(any(LeaveType.class));
        verify(policyRepository).save(any(LeaveEntitlementPolicy.class));
        verify(eligibilityRepository).save(any(LeaveEntitlementPolicyEligibilityRule.class));
        verify(leaveCalendarRepository).save(any(LeaveCalendar.class));
    }

    @Test
    void shouldAvoidDuplicateTemplateCopiesOnRetry() {
        Tenant tenant = Tenant.builder().id("acme").jurisdictionId("SG").build();
        Jurisdiction sg = Jurisdiction.builder().id("SG").code("SG").name("Singapore").build();
        JurisdictionLeaveType annual = JurisdictionLeaveType.builder()
                .id("SG:ANNUAL_LEAVE").jurisdictionId("SG").code("ANNUAL_LEAVE").name("Annual Leave").active(true).build();
        LeaveType existing = LeaveType.builder().id("acme:ANNUAL_LEAVE").tenantId("acme")
                .sourceJurisdictionLeaveTypeId(annual.getId()).name("Annual Leave").build();
        LeaveEntitlementPolicy template = LeaveEntitlementPolicy.builder()
                .id("template-annual").scope(ConfigurationScope.PLATFORM_TEMPLATE).jurisdictionId("SG")
                .jurisdictionLeaveTypeId(annual.getId()).name("Standard Annual Leave").active(true).build();
        LeaveCalendar calendar = LeaveCalendar.builder().id("sg-2026").scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG").start(LocalDate.of(2026, 1, 1)).end(LocalDate.of(2026, 12, 31)).build();

        when(jurisdictionRepository.findById("SG")).thenReturn(Optional.of(sg));
        when(jurisdictionLeaveTypeService.resolveEffective("SG")).thenReturn(List.of(annual));
        when(leaveTypeRepository.findAllByTenantId("acme")).thenReturn(List.of(existing));
        when(jurisdictionLeaveTypeRepository.findById(annual.getId())).thenReturn(Optional.of(annual));
        when(policyRepository.findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(template));
        when(policyRepository.existsByTenantIdAndSourceTemplateId("acme", template.getId())).thenReturn(true);
        when(leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG")).thenReturn(List.of(calendar));
        when(leaveCalendarRepository.existsByTenantIdAndSourceTemplateId("acme", calendar.getId())).thenReturn(true);

        service.provision(tenant);

        verify(leaveTypeRepository, never()).save(any());
        verify(policyRepository, never()).save(any());
        verify(eligibilityRepository, never()).save(any());
        verify(leaveCalendarRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingJurisdiction() {
        assertThatThrownBy(() -> service.provision(Tenant.builder().id("acme").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jurisdiction");
    }

    @Test
    void shouldRejectUnknownJurisdiction() {
        when(jurisdictionRepository.findById("XX")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.provision(Tenant.builder().id("acme").jurisdictionId("XX").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Jurisdiction not found");
    }
}
