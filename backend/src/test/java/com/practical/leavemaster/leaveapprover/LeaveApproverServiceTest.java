package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @InjectMocks
    private LeaveApproverService leaveApproverService;

    private Staff staff(String id) {
        return Staff.builder().id(id).name("Name " + id)
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

    @Test
    void shouldReturnAllLeaveApprovers() {
        when(leaveApproverRepository.findAll()).thenReturn(List.of(approver("id1"), approver("id2")));

        List<LeaveApprover> result = leaveApproverService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnLeaveApproverById() {
        LeaveApprover la = approver("id1");
        when(leaveApproverRepository.findById("id1")).thenReturn(Optional.of(la));

        Optional<LeaveApprover> result = leaveApproverService.findById("id1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("id1");
    }

    @Test
    void shouldReturnLeaveApproversByStaffId() {
        Staff s = staff("S001");
        when(staffRepository.findById("S001")).thenReturn(Optional.of(s));
        when(leaveApproverRepository.findByStaff(s)).thenReturn(List.of(approver("id1")));

        List<LeaveApprover> result = leaveApproverService.findByStaffId("S001");

        assertThat(result).hasSize(1);
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
        LeaveApprover saved = approver("id1");
        when(leaveApproverRepository.save(any(LeaveApprover.class))).thenReturn(saved);

        LeaveApprover result = leaveApproverService.create(request());

        assertThat(result.getId()).isEqualTo("id1");
        assertThat(result.getStaff().getId()).isEqualTo("S001");
        assertThat(result.getApprover().getId()).isEqualTo("S002");
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
        when(leaveApproverRepository.save(existing)).thenReturn(existing);

        LeaveApproverRequest updateRequest = LeaveApproverRequest.builder()
                .staffId("S001").approverId("S004").adminId("S003")
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2025, 6, 1))
                .build();

        LeaveApprover result = leaveApproverService.update("id1", updateRequest);

        assertThat(result.getApprover().getId()).isEqualTo("S004");
        assertThat(result.getEffectiveFrom()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(result.getEffectiveTo()).isEqualTo(LocalDate.of(2025, 6, 1));
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
