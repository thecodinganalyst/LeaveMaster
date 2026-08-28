package com.practical.leavemaster.staff;

import com.practical.leavemaster.config.ConfigurationScope;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyPeriodResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffEntitlementProposalServiceTest {

    private static final String SOURCE_LEAVE_TYPE_ID = "sg-annual";

    @Mock private LeaveCalendarService leaveCalendarService;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyResolutionService resolutionService;
    @Mock private StaffRepository staffRepository;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private StaffEntitlementProposalService proposalService;

    private final LocalDate joinDate = LocalDate.of(2026, 7, 1);
    private final LeaveCalendar calendar = LeaveCalendar.builder()
            .id("tenant-a:SG:2026")
            .jurisdictionId("SG")
            .start(LocalDate.of(2026, 1, 1))
            .end(LocalDate.of(2026, 12, 31))
            .tenantId("tenant-a")
            .build();

    @BeforeEach
    void authenticateTenantUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
        lenient().when(appUserRepository.findById("hr")).thenReturn(Optional.of(AppUser.builder()
                .loginName("hr")
                .password("n/a")
                .active(true)
                .tenantId("tenant-a")
                .build()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldProposeProratedAnnualEntitlementFromMatchingTemplatePolicy() {
        LeaveType annual = leaveType();
        LeaveEntitlementPolicy policy = templatePolicy("policy-annual", new BigDecimal("20.00"), AccrualMethod.ANNUAL, ProrationMethod.CALENDAR_DAYS);
        setupResolution(annual, "policy-annual");
        when(policyRepository.findById("policy-annual")).thenReturn(Optional.of(policy));

        List<LeaveEntitlement> result = proposalService.propose(
                new StaffEntitlementProposalRequest(null, " SG ", joinDate, null));

        assertThat(result).hasSize(1);
        LeaveEntitlement entitlement = result.getFirst();
        assertThat(entitlement.getLeaveType()).isEqualTo(annual);
        assertThat(entitlement.getFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(entitlement.getTo()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(entitlement.getEntitlement()).isEqualByComparingTo("10.00");
        assertThat(entitlement.getBaseEntitlementAmount()).isEqualByComparingTo("10.00");
        assertThat(entitlement.getCarriedForwardAmount()).isEqualByComparingTo("0.00");
        assertThat(entitlement.getPolicyId()).isEqualTo("policy-annual");
    }

    @Test
    void shouldProposePolicyThatBecomesEligibleLaterInCalendarPeriod() {
        LeaveType annual = leaveType();
        LeaveEntitlementPolicy policy = templatePolicy(
                "policy-future", new BigDecimal("12.00"), AccrualMethod.MONTHLY, ProrationMethod.NONE);
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(annual));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class)))
                .thenReturn(noMatch());
        LocalDate eligibleDate = LocalDate.of(2026, 11, 1);
        when(resolutionService.resolveTemplateInPeriod(
                any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class), eq(calendar.getEnd())))
                .thenReturn(new PolicyPeriodResolutionResult(
                        new PolicyResolutionResult("__preview__", SOURCE_LEAVE_TYPE_ID, "policy-future", false, "matched", List.of()),
                        eligibleDate,
                        true));
        when(policyRepository.findById("policy-future")).thenReturn(Optional.of(policy));

        LeaveEntitlement entitlement = proposalService.propose(
                new StaffEntitlementProposalRequest(null, "SG", joinDate, null)).getFirst();

        assertThat(entitlement.getFrom()).isEqualTo(eligibleDate);
        assertThat(entitlement.getTo()).isEqualTo(calendar.getEnd());
        assertThat(entitlement.getEntitlement()).isEqualByComparingTo("2.00");
        assertThat(entitlement.getPolicyId()).isEqualTo("policy-future");
    }

    @Test
    void shouldCalculateMonthlyAccrualAndRespectTerminationDate() {
        LeaveType annual = leaveType();
        LeaveEntitlementPolicy policy = templatePolicy("policy-monthly", new BigDecimal("12.00"), AccrualMethod.MONTHLY, ProrationMethod.NONE);
        LocalDate termDate = LocalDate.of(2026, 9, 30);
        setupResolution(annual, "policy-monthly");
        when(policyRepository.findById("policy-monthly")).thenReturn(Optional.of(policy));

        LeaveEntitlement entitlement = proposalService.propose(
                new StaffEntitlementProposalRequest(null, "SG", joinDate, termDate)).getFirst();

        assertThat(entitlement.getTo()).isEqualTo(termDate);
        assertThat(entitlement.getEntitlement()).isEqualByComparingTo("3.00");
    }

    @Test
    void shouldSkipLeaveTypesWithoutTemplateLineageOrMatchingPolicyInPeriod() {
        LeaveType manual = LeaveType.builder().id("manual").name("Manual").tenantId("tenant-a").build();
        LeaveType annual = leaveType();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(manual, annual));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class)))
                .thenReturn(noMatch());
        when(resolutionService.resolveTemplateInPeriod(
                any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class), eq(calendar.getEnd())))
                .thenReturn(new PolicyPeriodResolutionResult(noMatch(), null, true));

        assertThat(proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null))).isEmpty();
    }

    @Test
    void shouldRejectAmbiguousTemplatePolicy() {
        LeaveType annual = leaveType();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(annual));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class)))
                .thenReturn(new PolicyResolutionResult("__preview__", SOURCE_LEAVE_TYPE_ID, null, true, "ambiguous", List.of()));

        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same highest priority");
    }

    @Test
    void shouldRejectAmbiguousFutureTemplatePolicy() {
        LeaveType annual = leaveType();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(annual));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class)))
                .thenReturn(noMatch());
        when(resolutionService.resolveTemplateInPeriod(
                any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class), eq(calendar.getEnd())))
                .thenReturn(new PolicyPeriodResolutionResult(
                        new PolicyResolutionResult("__preview__", SOURCE_LEAVE_TYPE_ID, null, true, "ambiguous", List.of()),
                        LocalDate.of(2026, 11, 1),
                        true));

        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same highest priority");
    }

    @Test
    void shouldRejectJurisdictionWithoutTenantCalendar() {
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No leave calendar");
    }

    @Test
    void shouldRejectInvalidRequestDatesAndMissingFields() {
        assertThatThrownBy(() -> proposalService.propose(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, " ", joinDate, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, joinDate.minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldValidateExistingStaffBelongsToTenant() {
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(staffRepository.findById("S1")).thenReturn(Optional.of(Staff.builder()
                .id("S1").name("Staff").joinDate(joinDate).tenantId("tenant-b").build()));

        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest("S1", "SG", joinDate, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current tenant");
    }

    @Test
    void shouldRejectTenantOwnedOrMismatchedResolvedPolicies() {
        LeaveType annual = leaveType();
        setupResolution(annual, "policy");

        LeaveEntitlementPolicy tenantPolicy = templatePolicy("policy", BigDecimal.TEN, AccrualMethod.ANNUAL, ProrationMethod.NONE);
        tenantPolicy.setTenantId("tenant-a");
        when(policyRepository.findById("policy")).thenReturn(Optional.of(tenantPolicy));
        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null)))
                .hasMessageContaining("without a tenant id");

        LeaveEntitlementPolicy wrongSource = templatePolicy("policy", BigDecimal.TEN, AccrualMethod.ANNUAL, ProrationMethod.NONE);
        wrongSource.setJurisdictionLeaveTypeId("other");
        when(policyRepository.findById("policy")).thenReturn(Optional.of(wrongSource));
        assertThatThrownBy(() -> proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null)))
                .hasMessageContaining("does not match");
    }

    @Test
    void shouldSkipUnsupportedTemplatePolicyShapes() {
        LeaveType annual = leaveType();
        setupResolution(annual, "policy");

        LeaveEntitlementPolicy hours = templatePolicy("policy", BigDecimal.TEN, AccrualMethod.ANNUAL, ProrationMethod.NONE);
        hours.setEntitlementUnit(EntitlementUnit.HOURS);
        when(policyRepository.findById("policy")).thenReturn(Optional.of(hours));
        assertThat(proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null))).isEmpty();

        LeaveEntitlementPolicy perPayPeriod = templatePolicy("policy", BigDecimal.TEN, AccrualMethod.PER_PAY_PERIOD, ProrationMethod.NONE);
        when(policyRepository.findById("policy")).thenReturn(Optional.of(perPayPeriod));
        assertThat(proposalService.propose(new StaffEntitlementProposalRequest(null, "SG", joinDate, null))).isEmpty();
    }

    private void setupResolution(LeaveType leaveType, String policyId) {
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(leaveType));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class)))
                .thenReturn(new PolicyResolutionResult("__preview__", SOURCE_LEAVE_TYPE_ID, policyId, false, "matched", List.of()));
    }

    private PolicyResolutionResult noMatch() {
        return new PolicyResolutionResult("__preview__", SOURCE_LEAVE_TYPE_ID, null, false, "none", List.of());
    }

    private LeaveType leaveType() {
        return LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .tenantId("tenant-a")
                .sourceJurisdictionLeaveTypeId(SOURCE_LEAVE_TYPE_ID)
                .build();
    }

    private LeaveEntitlementPolicy templatePolicy(
            String id, BigDecimal amount, AccrualMethod accrualMethod, ProrationMethod prorationMethod) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .tenantId(null)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId(SOURCE_LEAVE_TYPE_ID)
                .name(id)
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(amount)
                .accrualMethod(accrualMethod)
                .prorationMethod(prorationMethod)
                .effectiveFrom(null)
                .effectiveTo(null)
                .build();
    }
}
