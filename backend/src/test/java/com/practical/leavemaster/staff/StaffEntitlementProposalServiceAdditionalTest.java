package com.practical.leavemaster.staff;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveentitlementpolicy.AccrualMethod;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyRepository;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicyResolutionService;
import com.practical.leavemaster.leaveentitlementpolicy.PolicyResolutionResult;
import com.practical.leavemaster.leaveentitlementpolicy.ProrationMethod;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffEntitlementProposalServiceAdditionalTest {

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

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRequireAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated tenant user is required");
    }

    @Test
    void shouldRejectUnknownAuthenticatedUser() {
        authenticatePrincipal();
        when(appUserRepository.findById("hr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    @Test
    void shouldRejectAuthenticatedUserWithoutTenant() {
        authenticatePrincipal();
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(user(null)));

        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant id");
    }

    @Test
    void shouldRejectUnknownExistingStaff() {
        authenticateTenantUser();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(staffRepository.findById("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.propose(request("S404")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown staff id");
    }

    @Test
    void shouldRejectResolvedPolicyThatNoLongerExists() {
        LeaveType annual = setupResolvedPolicy("policy-missing");
        when(policyRepository.findById("policy-missing")).thenReturn(Optional.empty());

        assertThat(annual.getId()).isEqualTo("annual");
        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer exists");
    }

    @Test
    void shouldRejectResolvedPolicyFromAnotherTenant() {
        setupResolvedPolicy("policy-foreign");
        when(policyRepository.findById("policy-foreign")).thenReturn(Optional.of(policy(
                "policy-foreign", "tenant-b", ProrationMethod.NONE)));

        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current tenant");
    }

    @Test
    void shouldProrateAnnualEntitlementByMonths() {
        setupResolvedPolicy("policy-months");
        when(policyRepository.findById("policy-months")).thenReturn(Optional.of(policy(
                "policy-months", "tenant-a", ProrationMethod.MONTHS)));

        LeaveEntitlement entitlement = proposalService.propose(request(null)).getFirst();

        assertThat(entitlement.getEntitlement()).isEqualByComparingTo("12.00");
    }

    private LeaveType setupResolvedPolicy(String policyId) {
        authenticateTenantUser();
        LeaveType annual = LeaveType.builder().id("annual").name("Annual Leave").tenantId("tenant-a").build();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(annual));
        when(resolutionService.resolve(any(Staff.class), eq("annual"), eq(calendar.getStart())))
                .thenReturn(new PolicyResolutionResult("__preview__", "annual", policyId, false, "matched", List.of()));
        return annual;
    }

    private LeaveEntitlementPolicy policy(String id, String tenantId, ProrationMethod prorationMethod) {
        return LeaveEntitlementPolicy.builder()
                .id(id)
                .tenantId(tenantId)
                .leaveTypeId("annual")
                .name(id)
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(new BigDecimal("24.00"))
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(prorationMethod)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
    }

    private StaffEntitlementProposalRequest request(String staffId) {
        return new StaffEntitlementProposalRequest(staffId, "SG", joinDate, null);
    }

    private void authenticatePrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
    }

    private void authenticateTenantUser() {
        authenticatePrincipal();
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(user("tenant-a")));
    }

    private AppUser user(String tenantId) {
        return AppUser.builder()
                .loginName("hr")
                .password("n/a")
                .active(true)
                .tenantId(tenantId)
                .build();
    }
}
