package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApproverServiceTest {

    @Mock
    private LeaveApproverRepository leaveApproverRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LeaveApproverService leaveApproverService;

    private Staff staff(String id) {
        return Staff.builder().id(id).name("Name " + id).tenantId("T1")
                .joinDate(LocalDate.of(2023, 1, 1)).build();
    }

    private LeaveApprover approver(String id) {
        return LeaveApprover.builder()
                .id(id)
                .staff(staff("S001"))
                .approver(staff("S002"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(staff("S003"))
                .adminDate(LocalDate.of(2023, 12, 1))
                .tenantId("T1")
                .build();
    }

    private LeaveApprover relationship(String id, String staffId, String approverId) {
        return LeaveApprover.builder()
                .id(id)
                .staff(staff(staffId))
                .approver(staff(approverId))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(staff("S003"))
                .adminDate(LocalDate.of(2023, 12, 1))
                .tenantId("T1")
                .build();
    }

    private LeaveApproverRequest request() {
        return LeaveApproverRequest.builder()
                .staffId("S001")
                .approverId("S002")
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .adminId("S003")
                .build();
    }

    private void allowApproval(String staffId) {
        AppPermission permission = AppPermission.builder()
                .code(RbacPermissions.LEAVE_APPLICATION_APPROVE)
                .description("Approve leave")
                .build();
        AppRole role = AppRole.builder()
                .id("APPROVER")
                .description("Approver")
                .active(true)
                .permissions(Set.of(permission))
                .build();
        AppUser user = AppUser.builder()
                .loginName(staffId)
                .password("ignored")
                .active(true)
                .staffId(staffId)
                .tenantId("T1")
                .roles(Set.of(role))
                .build();
        when(appUserRepository.findByStaffId(staffId)).thenReturn(Optional.of(user));
    }

    @Test
    void shouldReturnAllLeaveApprovers() {
        when(leaveApproverRepository.findAll()).thenReturn(List.of(approver("id1"), approver("id2")));
        assertThat(leaveApproverService.findAll()).hasSize(2);
    }

    @Test
    void shouldReturnLeaveApproverById() {
        LeaveApprover la = approver("id1");
        when(leaveApproverRepository.findById("id1")).thenReturn(Optional.of(la));
        assertThat(leaveApproverService.findById("id1")).contains(la);
    }

    @Test
    void shouldReturnLeaveApproversByStaffId() {
        Staff s = staff("S001");
        when(staffRepository.findById("S001")).thenReturn(Optional.of(s));
        when(leaveApproverRepository.findByStaff(s)).thenReturn(List.of(approver("id1")));
        assertThat(leaveApproverService.findByStaffId("S001")).hasSize(1);
    }

    @Test
    void shouldThrowWhenFindByStaffIdNotFound() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> leaveApproverService.findByStaffId("nonexistent"))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldCreateLeaveApprover() {
        Staff s1 = staff("S001");
        Staff s2 = staff("S002");
        Staff s3 = staff("S003");
        when(staffRepository.findById("S001")).thenReturn(Optional.of(s1));
        when(staffRepository.findById("S002")).thenReturn(Optional.of(s2));
        when(staffRepository.findById("S003")).thenReturn(Optional.of(s3));
        when(leaveApproverRepository.findAllByTenantId("T1")).thenReturn(List.of());
        allowApproval("S002");
        LeaveApprover saved = approver("id1");
        when(leaveApproverRepository.save(any(LeaveApprover.class))).thenReturn(saved);

        LeaveApprover result = leaveApproverService.create(request());

        assertThat(result.getId()).isEqualTo("id1");
        assertThat(result.getStaff().getId()).isEqualTo("S001");
        assertThat(result.getApprover().getId()).isEqualTo("S002");
    }

    @Test
    void shouldRejectApproverWithoutApprovalPermission() {
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff("S001")));
        when(staffRepository.findById("S002")).thenReturn(Optional.of(staff("S002")));
        when(staffRepository.findById("S003")).thenReturn(Optional.of(staff("S003")));
        when(appUserRepository.findByStaffId("S002")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveApproverService.create(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not authorised to approve leave");
    }

    @Test
    void shouldRejectSelfApproval() {
        LeaveApproverRequest req = request();
        req.setApproverId("S001");
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff("S001")));
        when(staffRepository.findById("S003")).thenReturn(Optional.of(staff("S003")));

        assertThatThrownBy(() -> leaveApproverService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("circular dependency");
    }

    @Test
    void shouldRejectIndirectCircularDependency() {
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff("S001")));
        when(staffRepository.findById("S002")).thenReturn(Optional.of(staff("S002")));
        when(staffRepository.findById("S003")).thenReturn(Optional.of(staff("S003")));
        allowApproval("S002");
        when(leaveApproverRepository.findAllByTenantId("T1")).thenReturn(List.of(
                relationship("r1", "S002", "S004"),
                relationship("r2", "S004", "S001")
        ));

        assertThatThrownBy(() -> leaveApproverService.create(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("circular dependency");
    }

    @Test
    void shouldThrowWhenCreateWithStaffNotFound() {
        when(staffRepository.findById("S001")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> leaveApproverService.create(request()))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldThrowWhenCreateWithMissingEffectiveFrom() {
        LeaveApproverRequest req = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S002").adminId("S003")
                .build();
        assertThatThrownBy(() -> leaveApproverService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveFrom is required");
    }

    @Test
    void shouldThrowWhenCreateWithInvalidDates() {
        LeaveApproverRequest req = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S002").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2024, 1, 1))
                .build();
        assertThatThrownBy(() -> leaveApproverService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveTo must be after effectiveFrom");
    }

    @Test
    void shouldUpdateLeaveApprover() {
        LeaveApprover existing = approver("id1");
        Staff s1 = staff("S001");
        Staff s4 = staff("S004");
        Staff s3 = staff("S003");
        when(leaveApproverRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(staffRepository.findById("S001")).thenReturn(Optional.of(s1));
        when(staffRepository.findById("S004")).thenReturn(Optional.of(s4));
        when(staffRepository.findById("S003")).thenReturn(Optional.of(s3));
        when(leaveApproverRepository.findAllByTenantId("T1")).thenReturn(List.of(existing));
        allowApproval("S004");
        when(leaveApproverRepository.save(existing)).thenReturn(existing);

        LeaveApproverRequest updateRequest = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S004").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2025, 6, 1))
                .build();

        LeaveApprover result = leaveApproverService.update("id1", updateRequest);
        assertThat(result.getApprover().getId()).isEqualTo("S004");
        assertThat(result.getEffectiveFrom()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLeaveApprover() {
        when(leaveApproverRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> leaveApproverService.update("nonexistent", request()))
                .isInstanceOf(LeaveApproverNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUpdateWithInvalidDates() {
        LeaveApproverRequest req = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S002").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2024, 1, 1))
                .build();
        assertThatThrownBy(() -> leaveApproverService.update("id1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveTo must be after effectiveFrom");
    }

    @Test
    void shouldDeleteLeaveApprover() {
        LeaveApprover la = approver("id1");
        when(leaveApproverRepository.findById("id1")).thenReturn(Optional.of(la));
        leaveApproverService.delete("id1");
        verify(leaveApproverRepository).deleteById("id1");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentLeaveApprover() {
        when(leaveApproverRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> leaveApproverService.delete("nonexistent"))
                .isInstanceOf(LeaveApproverNotFoundException.class);
        verify(leaveApproverRepository, never()).deleteById("nonexistent");
    }
}
