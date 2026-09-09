package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffTenantIsolationTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private LeaveCalendarService leaveCalendarService;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private LeaveApproverRepository leaveApproverRepository;
    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private TenantActivityService tenantActivityService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private StaffService staffService;

    @BeforeEach
    void authenticateTenantBAdmin() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("tenant-b-admin");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        AppUser tenantAdmin = AppUser.builder()
                .userId("tenant-b-admin")
                .loginName("tenant-b-admin")
                .tenantId("TENANT_B")
                .active(true)
                .build();
        when(appUserRepository.findById("tenant-b-admin")).thenReturn(Optional.of(tenantAdmin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListOnlyStaffFromAuthenticatedTenant() {
        Staff tenantBStaff = Staff.builder()
                .id("B001")
                .tenantId("TENANT_B")
                .name("Tenant B Staff")
                .joinDate(LocalDate.of(2026, 1, 1))
                .build();
        when(staffRepository.findAllByTenantId("TENANT_B")).thenReturn(List.of(tenantBStaff));
        when(appUserService.findRoleIdsByStaffId("B001")).thenReturn(Set.of());

        List<Staff> result = staffService.findAll();

        assertThat(result).containsExactly(tenantBStaff);
        verify(staffRepository).findAllByTenantId("TENANT_B");
        verify(staffRepository, never()).findAll();
    }

    @Test
    void shouldNotReadStaffFromAnotherTenantById() {
        when(staffRepository.findByIdAndTenantId("A001", "TENANT_B")).thenReturn(Optional.empty());

        Optional<Staff> result = staffService.findById("A001");

        assertThat(result).isEmpty();
        verify(staffRepository).findByIdAndTenantId("A001", "TENANT_B");
        verify(staffRepository, never()).findById("A001");
    }

    @Test
    void shouldNotUpdateStaffFromAnotherTenantById() {
        when(staffRepository.findByIdAndTenantId("A001", "TENANT_B")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.update("A001", Staff.builder().name("Changed").build()))
                .isInstanceOf(StaffNotFoundException.class);

        verify(staffRepository, never()).save(org.mockito.ArgumentMatchers.any(Staff.class));
    }

    @Test
    void shouldNotDeleteStaffFromAnotherTenantById() {
        when(staffRepository.findByIdAndTenantId("A001", "TENANT_B")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.delete("A001"))
                .isInstanceOf(StaffNotFoundException.class);

        verify(staffRepository, never()).deleteById("A001");
    }

    @Test
    void shouldNotTerminateStaffFromAnotherTenantById() {
        when(staffRepository.findByIdAndTenantId("A001", "TENANT_B")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.terminate("A001", LocalDate.of(2026, 9, 10)))
                .isInstanceOf(StaffNotFoundException.class);

        verify(staffRepository, never()).save(org.mockito.ArgumentMatchers.any(Staff.class));
    }
}
