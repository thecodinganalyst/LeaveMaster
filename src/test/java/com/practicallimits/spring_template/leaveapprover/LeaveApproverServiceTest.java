package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.StaffRepository;
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

    @InjectMocks
    private LeaveApproverService leaveApproverService;

    private Staff staff(String id) {
        return Staff.builder().id(id).name("Name " + id)
                .joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
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
    void shouldSaveLeaveApprover() {
        LeaveApprover la = approver("id1");
        when(leaveApproverRepository.save(la)).thenReturn(la);

        LeaveApprover result = leaveApproverService.save(la);

        assertThat(result.getId()).isEqualTo("id1");
    }

    @Test
    void shouldUpdateLeaveApprover() {
        LeaveApprover existing = approver("id1");
        LeaveApprover updated = LeaveApprover.builder()
                .id("id1")
                .staff(staff("S001"))
                .approver(staff("S004"))
                .effectiveFrom(LocalDate.of(2024, 6, 1))
                .effectiveTo(LocalDate.of(2025, 6, 1))
                .admin(staff("S003"))
                .adminDate(LocalDate.of(2024, 5, 1))
                .build();
        when(leaveApproverRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(leaveApproverRepository.save(existing)).thenReturn(existing);

        LeaveApprover result = leaveApproverService.update("id1", updated);

        assertThat(result.getApprover().getId()).isEqualTo("S004");
        assertThat(result.getEffectiveFrom()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(result.getEffectiveTo()).isEqualTo(LocalDate.of(2025, 6, 1));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLeaveApprover() {
        when(leaveApproverRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveApproverService.update("nonexistent", new LeaveApprover()))
                .isInstanceOf(LeaveApproverNotFoundException.class);
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
