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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffEntitlementPriorYearJoinTest {

    private static final String SOURCE_LEAVE_TYPE_ID = "sg-annual";

    @Mock private LeaveCalendarService leaveCalendarService;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveEntitlementPolicyRepository policyRepository;
    @Mock private LeaveEntitlementPolicyResolutionService resolutionService;
    @Mock private StaffRepository staffRepository;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private StaffEntitlementProposalService proposalService;

    @BeforeEach
    void authenticateTenantUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(AppUser.builder()
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
    void shouldUseCurrentCalendarAndFullPeriodForPriorYearJoinDate() {
        LocalDate today = LocalDate.now();
        LocalDate priorYearJoinDate = today.minusYears(1);
        LocalDate periodStart = LocalDate.of(today.getYear(), 1, 1);
        LocalDate periodEnd = LocalDate.of(today.getYear(), 12, 31);
        LeaveCalendar calendar = LeaveCalendar.builder()
                .id("tenant-a:SG:" + today.getYear())
                .jurisdictionId("SG")
                .start(periodStart)
                .end(periodEnd)
                .tenantId("tenant-a")
                .build();
        LeaveType annual = LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .tenantId("tenant-a")
                .sourceJurisdictionLeaveTypeId(SOURCE_LEAVE_TYPE_ID)
                .build();
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder()
                .id("policy-annual")
                .tenantId(null)
                .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                .jurisdictionId("SG")
                .jurisdictionLeaveTypeId(SOURCE_LEAVE_TYPE_ID)
                .name("Annual Leave")
                .active(true)
                .priority(10)
                .entitlementUnit(EntitlementUnit.DAYS)
                .entitlementAmount(new BigDecimal("20.00"))
                .accrualMethod(AccrualMethod.ANNUAL)
                .prorationMethod(ProrationMethod.CALENDAR_DAYS)
                .build();

        when(leaveCalendarService.getCalendarFor("SG", today)).thenReturn(Optional.of(calendar));
        when(leaveTypeRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(annual));
        when(resolutionService.resolveTemplate(any(Staff.class), eq(SOURCE_LEAVE_TYPE_ID), any(LocalDate.class)))
                .thenReturn(new PolicyResolutionResult(
                        "__preview__", SOURCE_LEAVE_TYPE_ID, "policy-annual", false, "matched", List.of()));
        when(policyRepository.findById("policy-annual")).thenReturn(Optional.of(policy));

        LeaveEntitlement entitlement = proposalService.propose(
                new StaffEntitlementProposalRequest(null, "SG", priorYearJoinDate, null)).getFirst();

        verify(leaveCalendarService).getCalendarFor("SG", today);
        assertThat(entitlement.getFrom()).isEqualTo(periodStart);
        assertThat(entitlement.getTo()).isEqualTo(periodEnd);
        assertThat(entitlement.getEntitlement()).isEqualByComparingTo("20.00");
        assertThat(entitlement.getBaseEntitlementAmount()).isEqualByComparingTo("20.00");
    }
}
