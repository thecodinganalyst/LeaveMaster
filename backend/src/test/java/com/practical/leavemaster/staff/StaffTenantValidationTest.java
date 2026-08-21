package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffTenantValidationTest {

    @Mock private StaffRepository staffRepository;
    @Mock private LeaveCalendarService leaveCalendarService;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveApproverRepository leaveApproverRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private AppUserService appUserService;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private StaffService staffService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldApplyAuthenticatedTenantAndAcceptConfiguredJurisdiction() {
        authenticate("tenant-a");
        LocalDate joinDate = LocalDate.of(2026, 1, 1);
        Staff staff = Staff.builder().id("S1").name("Alice").joinDate(joinDate).jurisdictionId(" SG ").build();
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar(joinDate)));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff saved = staffService.save(staff);

        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.getJurisdictionId()).isEqualTo("SG");
        verify(appUserService).createForStaff("S1", "S1", "S1", true, "tenant-a");
        verify(tenantActivityService).touch("tenant-a");
    }

    @Test
    void shouldRejectMissingOrUnconfiguredJurisdiction() {
        authenticate("tenant-a");
        LocalDate joinDate = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> staffService.save(Staff.builder()
                .id("S1").name("Alice").joinDate(joinDate).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jurisdictionId");

        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> staffService.save(Staff.builder()
                .id("S2").name("Bob").joinDate(joinDate).jurisdictionId("SG").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leave calendar");
    }

    @Test
    void shouldRejectLeaveTypeFromAnotherTenant() {
        authenticate("tenant-a");
        LocalDate joinDate = LocalDate.of(2026, 1, 1);
        when(leaveCalendarService.getCalendarFor("SG", joinDate)).thenReturn(Optional.of(calendar(joinDate)));
        when(leaveTypeRepository.findById("foreign")).thenReturn(Optional.of(
                LeaveType.builder().id("foreign").name("Foreign").tenantId("tenant-b").build()));
        Staff staff = Staff.builder()
                .id("S1").name("Alice").joinDate(joinDate).jurisdictionId("SG")
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("foreign").build())
                        .from(joinDate).to(LocalDate.of(2026, 12, 31))
                        .entitlement(BigDecimal.TEN)
                        .build()))
                .build();

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("staff tenant");
    }

    @Test
    void shouldRejectUpdatingStaffOwnedByAnotherTenant() {
        authenticate("tenant-a");
        LocalDate joinDate = LocalDate.of(2026, 1, 1);
        Staff existing = Staff.builder()
                .id("S1").name("Alice").joinDate(joinDate).jurisdictionId("SG").tenantId("tenant-b").build();
        when(staffRepository.findById("S1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> staffService.update("S1", Staff.builder()
                .name("Updated").joinDate(joinDate).jurisdictionId("SG").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current tenant");
    }

    @Test
    void shouldRejectTenantUserWithoutTenantId() {
        authenticate(null);
        assertThatThrownBy(() -> staffService.save(Staff.builder()
                .id("S1").name("Alice").joinDate(LocalDate.of(2026, 1, 1)).jurisdictionId("SG").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant id");
    }

    private void authenticate(String tenantId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr", "n/a", List.of()));
        when(appUserRepository.findById("hr")).thenReturn(Optional.of(AppUser.builder()
                .loginName("hr")
                .password("n/a")
                .active(true)
                .tenantId(tenantId)
                .build()));
    }

    private LeaveCalendar calendar(LocalDate date) {
        return LeaveCalendar.builder()
                .id("tenant-a:SG:2026")
                .tenantId("tenant-a")
                .jurisdictionId("SG")
                .start(date.withDayOfYear(1))
                .end(date.withMonth(12).withDayOfMonth(31))
                .build();
    }
}
