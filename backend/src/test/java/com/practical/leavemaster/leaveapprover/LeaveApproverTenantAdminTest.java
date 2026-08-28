package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveApproverTenantAdminTest {

    @Mock
    private LeaveApproverRepository leaveApproverRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private TenantActivityService tenantActivityService;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LeaveApproverService service;

    private Staff staff;
    private Staff approver;
    private AppUser tenantAdmin;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Bravo_Admin", "password", List.of()));

        staff = Staff.builder().id("001").name("Normal Staff").tenantId("Bravo").joinDate(LocalDate.of(2026, 8, 3)).build();
        approver = Staff.builder().id("002").name("Manager Staff").tenantId("Bravo").joinDate(LocalDate.of(2026, 1, 1)).build();
        tenantAdmin = AppUser.builder().loginName("Bravo_Admin").active(true).tenantId("Bravo").staffId(null).build();

        AppPermission approve = AppPermission.builder()
                .code(RbacPermissions.LEAVE_APPLICATION_APPROVE)
                .description("Approve leave")
                .build();
        AppRole manager = AppRole.builder()
                .id("Bravo_Manager")
                .description("Manager")
                .active(true)
                .tenantId("Bravo")
                .permissions(Set.of(approve))
                .build();
        AppUser approverUser = AppUser.builder()
                .loginName("manager")
                .active(true)
                .staffId("002")
                .tenantId("Bravo")
                .roles(Set.of(manager))
                .build();

        when(appUserRepository.findById("Bravo_Admin")).thenReturn(Optional.of(tenantAdmin));
        lenient().when(appUserRepository.findByStaffId("002")).thenReturn(Optional.of(approverUser));
        when(staffRepository.findById("001")).thenReturn(Optional.of(staff));
        when(staffRepository.findById("002")).thenReturn(Optional.of(approver));
        lenient().when(leaveApproverRepository.findAllByTenantId("Bravo")).thenReturn(List.of());
        lenient().when(leaveApproverRepository.save(any(LeaveApprover.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantAdminWithoutStaffRecordCanCreateAssignmentAndIsAuditedByLoginName() {
        LeaveApprover result = service.create(request());

        assertThat(result.getStaff()).isSameAs(staff);
        assertThat(result.getApprover()).isSameAs(approver);
        assertThat(result.getAdmin()).isNull();
        assertThat(result.getAdminLoginName()).isEqualTo("Bravo_Admin");
        assertThat(result.getTenantId()).isEqualTo("Bravo");
        assertThat(result.getAdminDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void tenantAdminWithoutStaffRecordCanUpdateAssignmentAndReplacesAuditActor() {
        Staff legacyAdmin = Staff.builder().id("900").name("Old Admin").tenantId("Bravo").joinDate(LocalDate.of(2025, 1, 1)).build();
        LeaveApprover existing = LeaveApprover.builder()
                .id("assignment-1")
                .staff(staff)
                .approver(approver)
                .admin(legacyAdmin)
                .adminDate(LocalDate.of(2026, 1, 1))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .tenantId("Bravo")
                .build();
        when(leaveApproverRepository.findById("assignment-1")).thenReturn(Optional.of(existing));

        LeaveApprover result = service.update("assignment-1", request());

        assertThat(result.getAdmin()).isNull();
        assertThat(result.getAdminLoginName()).isEqualTo("Bravo_Admin");
        assertThat(result.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    void tenantAdminCannotAssignApproverFromAnotherTenant() {
        Staff foreignApprover = Staff.builder().id("002").name("Foreign Manager").tenantId("Other").joinDate(LocalDate.of(2026, 1, 1)).build();
        when(staffRepository.findById("002")).thenReturn(Optional.of(foreignApprover));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Approver does not belong to the current tenant");
    }

    private LeaveApproverRequest request() {
        return LeaveApproverRequest.builder()
                .staffId("001")
                .approverId("002")
                .effectiveFrom(LocalDate.of(2026, 8, 28))
                .build();
    }
}
