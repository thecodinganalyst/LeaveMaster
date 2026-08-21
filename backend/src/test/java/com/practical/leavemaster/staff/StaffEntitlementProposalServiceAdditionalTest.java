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
    void shouldRejectResolvedTemplateThatNoLongerExists() {
        setupResolvedTemplate("policy-missing");
        when(policyRepository.findById("policy-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("template no longer exists");
    }

    @Test
    void shouldRejectResolvedPolicyWithNonTemplateScope() {
        setupResolvedTemplate("policy-invalid");
        LeaveEntitlementPolicy invalid = policy("policy-invalid", ProrationMethod.NONE);
        invalid.setScope(ConfigurationScope.TENANT);
        when(policyRepository.findById("policy-invalid")).thenReturn(Optional.of(invalid));

        assertThatThrownBy(() -> proposalService.propose(request(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PLATFORM_TEMPLATE");
    }

    @Test
    void shouldProrateAnnualEntitlementByMonths() {
        setupResolvedTemplate("policy-months");
        when(policyRepository.findById("policy-months")).thenReturn(Optional.of(policy(
                "policy-months", ProrationMethod.MONTHS)));

        LeaveEntitlement entitlement = proposalService.propose(request(null)).getFirst();

        assertThat(entitlement.getEntitlement()).isEqualByComparingTo("12.00");
    }

    private void setupResolvedTemplate(String policyId) {
        authenticateTenantUser();
        LeaveType annual = LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .tenantId("tenant-a")
                .sourceJurisdictionLeaveTypeId(SOURCE_LEAVE_TYPE_ID)
                .build();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(annual));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), eq(calendar.getStart())))
                .thenReturn(new PolicyResolutionResult("__preview__", SOURCE_LEAVE_TYPE_ID, policyId, false, "matched", List.of()));
    }

    private LeaveEntitlementPolicy policy(String id, ProrationMethod prorationMethod) {
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
