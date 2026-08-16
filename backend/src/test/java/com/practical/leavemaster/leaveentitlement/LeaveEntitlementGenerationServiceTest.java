package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyValidationException;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEntitlementGenerationServiceTest {
    @Mock private StaffRepository staffRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementRepository entitlementRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyResolutionService resolutionService;
    @Mock private LeaveApplicationRepository applicationRepository;
    @Mock private AppUserRepository appUserRepository;

    private LeaveEntitlementGenerationService service;
    private Staff staff;
    private LeaveType leaveType;
    private LocalDate start;
    private LocalDate end;

    @BeforeEach
    void setUp() {
        service = new LeaveEntitlementGenerationService(staffRepository, leaveTypeRepository, entitlementRepository,
                policyRepository, resolutionService, applicationRepository, appUserRepository);
        start = LocalDate.of(2027, 1, 1);
        end = LocalDate.of(2027, 12, 31);
        leaveType = LeaveType.builder().id("annual").name("Annual Leave").tenantId("tenant-a").build();
        staff = Staff.builder().id("staff-1").name("Staff One").tenantId("tenant-a")
                .joinDate(LocalDate.of(2026, 1, 1)).leaveEntitlements(new ArrayList<>()).build();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatesNewEntitlementFromWinningPolicy() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", new BigDecimal("14"), ProrationMethod.NONE);
        select(policy);
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.empty());
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(entitlementRepository.save(any())).thenAnswer(invocation -> {
            LeaveEntitlement saved = invocation.getArgument(0);
            saved.setId("e1");
            return saved;
        });

        EntitlementGenerationResult result = service.generateForStaff("staff-1", start, end).getFirst();

        assertThat(result.status()).isEqualTo(EntitlementGenerationResult.Status.CREATED);
        assertThat(result.policyId()).isEqualTo("p1");
        assertThat(result.entitlementAmount()).isEqualByComparingTo("14.00");
        assertThat(staff.getLeaveEntitlements()).hasSize(1);
    }

    @Test
    void rerunUpdatesSameRecordAndPreservesAdjustment() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", new BigDecimal("14"), ProrationMethod.NONE);
        select(policy);
        LeaveEntitlement existing = entitlement("e1", "p1", new BigDecimal("12"));
        existing.setAdjustmentAmount(new BigDecimal("2"));
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.of(existing));
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(entitlementRepository.save(existing)).thenReturn(existing);

        EntitlementGenerationResult result = service.generateForStaff("staff-1", start, end).getFirst();

        assertThat(result.status()).isEqualTo(EntitlementGenerationResult.Status.UPDATED);
        assertThat(existing.getEntitlement()).isEqualByComparingTo("16.00");
        assertThat(existing.getAdjustmentAmount()).isEqualByComparingTo("2");
        verify(entitlementRepository).save(existing);
    }

    @Test
    void protectsLegacyAndHistoricalEntitlements() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", new BigDecimal("14"), ProrationMethod.NONE);
        select(policy);
        LeaveEntitlement legacy = entitlement("legacy", null, new BigDecimal("10"));
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.of(legacy));
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(service.generateForStaff("staff-1", start, end).getFirst().status())
                .isEqualTo(EntitlementGenerationResult.Status.LEGACY_PROTECTED);
        verify(entitlementRepository, never()).save(any());

        LocalDate pastStart = LocalDate.of(2025, 1, 1);
        LocalDate pastEnd = LocalDate.of(2025, 12, 31);
        LeaveEntitlement historical = entitlement("old", "p1", new BigDecimal("14"));
        historical.setFrom(pastStart);
        historical.setTo(pastEnd);
        when(resolutionService.resolve("staff-1", "annual", pastStart)).thenReturn(selected("p1"));
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, pastStart, pastEnd))
                .thenReturn(Optional.of(historical));

        assertThat(service.generateForStaff("staff-1", pastStart, pastEnd).getFirst().status())
                .isEqualTo(EntitlementGenerationResult.Status.HISTORICAL_PROTECTED);
    }

    @Test
    void reportsNoMatchAndAmbiguityWithoutWriting() {
        stubStaffAndTypes();
        when(resolutionService.resolve("staff-1", "annual", start))
                .thenReturn(new PolicyResolutionResult("staff-1", "annual", null, false, "none", List.of()));
        assertThat(service.generateForStaff("staff-1", start, end).getFirst().status())
                .isEqualTo(EntitlementGenerationResult.Status.NO_MATCHING_POLICY);

        when(resolutionService.resolve("staff-1", "annual", start))
                .thenReturn(new PolicyResolutionResult("staff-1", "annual", null, true, "ambiguous", List.of()));
        assertThat(service.generateForStaff("staff-1", start, end).getFirst().status())
                .isEqualTo(EntitlementGenerationResult.Status.AMBIGUOUS_POLICY);
        verify(entitlementRepository, never()).save(any());
    }

    @Test
    void proratesByCalendarDaysAndMonths() {
        staff.setJoinDate(LocalDate.of(2027, 7, 1));
        stubStaffAndTypes();
        LeaveEntitlementPolicy calendarPolicy = policy("p1", new BigDecimal("12"), ProrationMethod.CALENDAR_DAYS);
        select(calendarPolicy);
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.empty());
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(entitlementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal calendarAmount = service.generateForStaff("staff-1", start, end).getFirst().baseAmount();
        assertThat(calendarAmount).isEqualByComparingTo("6.05");

        LeaveEntitlementPolicy monthsPolicy = policy("p2", new BigDecimal("12"), ProrationMethod.MONTHS);
        select(monthsPolicy);
        BigDecimal monthsAmount = service.generateForStaff("staff-1", start, end).getFirst().baseAmount();
        assertThat(monthsAmount).isEqualByComparingTo("6.00");
    }

    @Test
    void monthlyAccrualDerivesRateAndDoesNotDoubleProrate() {
        staff.setJoinDate(LocalDate.of(2027, 3, 15));
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", new BigDecimal("12"), ProrationMethod.CALENDAR_DAYS);
        policy.setAccrualMethod(AccrualMethod.MONTHLY);
        policy.setAccrualRate(new BigDecimal("99"));
        select(policy);
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.empty());
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(entitlementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.generateForStaff("staff-1", start, end).getFirst().baseAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void monthlyAccrualCapsAtEntitlementAmountForFullYear() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", new BigDecimal("14"), ProrationMethod.NONE);
        policy.setAccrualMethod(AccrualMethod.MONTHLY);
        select(policy);
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.empty());
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(entitlementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.generateForStaff("staff-1", start, end).getFirst().baseAmount()).isEqualByComparingTo("14.00");
    }

    @Test
    void carryForwardUsesPreviousAvailableBalanceAndLimit() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", new BigDecimal("14"), ProrationMethod.NONE);
        policy.setCarryForwardAllowed(true);
        policy.setCarryForwardLimit(new BigDecimal("5"));
        policy.setCarryForwardExpiryMonths(12);
        select(policy);
        LeaveEntitlement previous = entitlement("prev", "p1", new BigDecimal("10"));
        previous.setFrom(LocalDate.of(2026, 1, 1));
        previous.setTo(LocalDate.of(2026, 12, 31));
        LeaveApplication approved = application(LeaveStatus.APPROVED, LeaveDuration.FULL);
        when(entitlementRepository.findAllByStaffAndLeaveTypeAndToBeforeOrderByToDesc(staff, leaveType, start))
                .thenReturn(List.of(previous));
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.empty());
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(staff, leaveType, previous.getFrom(), previous.getTo(),
                List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING))).thenReturn(List.of(approved));
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(staff, leaveType, start, end,
                List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING))).thenReturn(List.of());
        when(entitlementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EntitlementGenerationResult result = service.generateForStaff("staff-1", start, end).getFirst();
        assertThat(result.carriedForwardAmount()).isEqualByComparingTo("5.00");
        assertThat(result.entitlementAmount()).isEqualByComparingTo("19.00");
    }

    @Test
    void rejectsRecalculationBelowUsedAndReservedLeave() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy policy = policy("p1", BigDecimal.ONE, ProrationMethod.NONE);
        select(policy);
        LeaveEntitlement existing = entitlement("e1", "p1", new BigDecimal("5"));
        LeaveApplication approved = application(LeaveStatus.APPROVED, LeaveDuration.FULL);
        LeaveApplication pendingHalf = application(LeaveStatus.PENDING, LeaveDuration.AM);
        when(entitlementRepository.findByStaffAndLeaveTypeAndFromAndTo(staff, leaveType, start, end)).thenReturn(Optional.of(existing));
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(), any(), any(), any(), any()))
                .thenReturn(List.of(approved, pendingHalf));

        assertThatThrownBy(() -> service.generateForStaff("staff-1", start, end))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class)
                .hasMessageContaining("used/reserved");
    }

    @Test
    void rejectsUnsupportedUnitAndPayPeriodAccrual() {
        stubStaffAndTypes();
        LeaveEntitlementPolicy hours = policy("p1", new BigDecimal("80"), ProrationMethod.NONE);
        hours.setEntitlementUnit(EntitlementUnit.HOURS);
        select(hours);
        assertThatThrownBy(() -> service.generateForStaff("staff-1", start, end))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("DAYS");

        LeaveEntitlementPolicy payPeriod = policy("p2", new BigDecimal("14"), ProrationMethod.NONE);
        payPeriod.setAccrualMethod(AccrualMethod.PER_PAY_PERIOD);
        select(payPeriod);
        assertThatThrownBy(() -> service.generateForStaff("staff-1", start, end))
                .isInstanceOf(LeaveEntitlementPolicyValidationException.class).hasMessageContaining("payroll schedule");
    }

    @Test
    void tenantBatchEnforcesTenantBoundary() {
        authenticateTenantUser("hr", "tenant-a");
        when(staffRepository.findAllByTenantId("tenant-a")).thenReturn(List.of());
        assertThat(service.generateForTenant("tenant-a", start, end)).isEmpty();

        assertThatThrownBy(() -> service.generateForTenant("tenant-b", start, end))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("denied");
    }

    @Test
    void validatesPeriodAndUnknownStaff() {
        assertThatThrownBy(() -> service.generateForStaff("x", end, start))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("period");
        when(staffRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generateForStaff("missing", start, end))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown staff");
    }

    private void stubStaffAndTypes() {
        when(staffRepository.findById("staff-1")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(leaveType));
    }

    private void select(LeaveEntitlementPolicy policy) {
        when(resolutionService.resolve("staff-1", "annual", start)).thenReturn(selected(policy.getId()));
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
    }

    private PolicyResolutionResult selected(String policyId) {
        return new PolicyResolutionResult("staff-1", "annual", policyId, false, "selected", List.of());
    }

    private LeaveEntitlementPolicy policy(String id, BigDecimal amount, ProrationMethod prorationMethod) {
        return LeaveEntitlementPolicy.builder().id(id).tenantId("tenant-a").leaveTypeId("annual").name("Annual")
                .active(true).priority(10).entitlementUnit(EntitlementUnit.DAYS).entitlementAmount(amount)
                .accrualMethod(AccrualMethod.ANNUAL).prorationMethod(prorationMethod).carryForwardAllowed(false)
                .effectiveFrom(LocalDate.of(2026, 1, 1)).build();
    }

    private LeaveEntitlement entitlement(String id, String policyId, BigDecimal amount) {
        return LeaveEntitlement.builder().id(id).staff(staff).leaveType(leaveType).tenantId("tenant-a")
                .from(start).to(end).policyId(policyId).entitlement(amount).baseEntitlementAmount(amount)
                .carriedForwardAmount(BigDecimal.ZERO).adjustmentAmount(BigDecimal.ZERO).build();
    }

    private LeaveApplication application(LeaveStatus status, LeaveDuration duration) {
        LeaveApplication application = mock(LeaveApplication.class);
        when(application.getStatus()).thenReturn(status);
        when(application.getLeaveDuration()).thenReturn(duration);
        return application;
    }

    private void authenticateTenantUser(String login, String tenantId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
        when(appUserRepository.findById(login)).thenReturn(Optional.of(AppUser.builder()
                .loginName(login).active(true).tenantId(tenantId).roles(Set.<AppRole>of()).build()));
    }
}
