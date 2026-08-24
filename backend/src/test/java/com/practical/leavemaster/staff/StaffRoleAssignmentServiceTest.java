package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffRoleAssignmentServiceTest {

    @Mock private StaffRepository staffRepository;
    @Mock private LeaveCalendarService leaveCalendarService;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveApproverRepository leaveApproverRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private AppUserService appUserService;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private StaffService staffService;

    @Test
    void shouldPassMultipleRolesWhenCreatingStaffUser() {
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice")
                .joinDate(LocalDate.of(2025, 1, 1))
                .tenantId("tenant-a")
                .roleIds(Set.of("EMPLOYEE", "APPROVER"))
                .build();
        when(staffRepository.save(staff)).thenReturn(staff);

        Staff saved = staffService.save(staff);

        assertThat(saved.getRoleIds()).containsExactlyInAnyOrder("EMPLOYEE", "APPROVER");
        verify(appUserService).createForStaff(
                eq("S001"), eq("S001"), eq("S001"), anyBoolean(), eq("tenant-a"),
                eq(Set.of("EMPLOYEE", "APPROVER")));
    }

    @Test
    void shouldReplaceRolesWhenUpdatingStaff() {
        Staff existing = Staff.builder()
                .id("S001")
                .name("Alice")
                .joinDate(LocalDate.of(2025, 1, 1))
                .tenantId("tenant-a")
                .build();
        Staff update = Staff.builder()
                .id("S001")
                .name("Alice")
                .joinDate(LocalDate.of(2025, 1, 1))
                .roleIds(Set.of("APPROVER", "HR_ADMIN"))
                .build();
        when(staffRepository.findById("S001")).thenReturn(java.util.Optional.of(existing));
        when(staffRepository.save(existing)).thenReturn(existing);
        when(appUserService.findRoleIdsByStaffId("S001")).thenReturn(Set.of("APPROVER", "HR_ADMIN"));

        Staff saved = staffService.update("S001", update);

        verify(appUserService).updateRolesByStaffId("S001", Set.of("APPROVER", "HR_ADMIN"), "tenant-a");
        assertThat(saved.getRoleIds()).containsExactlyInAnyOrder("APPROVER", "HR_ADMIN");
    }

    @Test
    void shouldHydrateAssignedRolesForStaffDetails() {
        Staff staff = Staff.builder().id("S001").name("Alice").joinDate(LocalDate.of(2025, 1, 1)).build();
        when(staffRepository.findById("S001")).thenReturn(java.util.Optional.of(staff));
        when(appUserService.findRoleIdsByStaffId("S001")).thenReturn(Set.of("EMPLOYEE", "APPROVER"));

        Staff result = staffService.findById("S001").orElseThrow();

        assertThat(result.getRoleIds()).containsExactlyInAnyOrder("EMPLOYEE", "APPROVER");
    }
}
