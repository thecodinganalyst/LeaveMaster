package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LeaveApproverRepositoryTest {

    @Autowired
    private LeaveApproverRepository leaveApproverRepository;

    @Autowired
    private StaffRepository staffRepository;

    private Staff savedStaff(String id, String name) {
        return staffRepository.save(Staff.builder()
                .id(id)
                .name(name)
                .joinDate(LocalDate.of(2023, 1, 1))
                .build());
    }

    @Test
    void shouldSaveAndFindLeaveApprover() {
        Staff staff = savedStaff("S001", "Alice Smith");
        Staff approver = savedStaff("S002", "Bob Jones");
        Staff admin = savedStaff("S003", "Carol White");

        LeaveApprover leaveApprover = LeaveApprover.builder()
                .staff(staff)
                .approver(approver)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(admin)
                .adminDate(LocalDate.of(2023, 12, 1))
                .build();

        LeaveApprover saved = leaveApproverRepository.save(leaveApprover);
        assertThat(saved.getId()).isNotNull();

        Optional<LeaveApprover> found = leaveApproverRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStaff().getId()).isEqualTo("S001");
        assertThat(found.get().getApprover().getId()).isEqualTo("S002");
        assertThat(found.get().getEffectiveFrom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(found.get().getEffectiveTo()).isNull();
    }

    @Test
    void shouldFindByStaff() {
        Staff staff = savedStaff("S001", "Alice Smith");
        Staff approver1 = savedStaff("S002", "Bob Jones");
        Staff approver2 = savedStaff("S003", "Carol White");
        Staff admin = savedStaff("S004", "Dave Brown");

        leaveApproverRepository.save(LeaveApprover.builder()
                .staff(staff).approver(approver1)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(admin).adminDate(LocalDate.of(2023, 12, 1))
                .build());
        leaveApproverRepository.save(LeaveApprover.builder()
                .staff(staff).approver(approver2)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(admin).adminDate(LocalDate.of(2023, 12, 1))
                .build());

        List<LeaveApprover> results = leaveApproverRepository.findByStaff(staff);
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldDeleteLeaveApprover() {
        Staff staff = savedStaff("S001", "Alice Smith");
        Staff approver = savedStaff("S002", "Bob Jones");
        Staff admin = savedStaff("S003", "Carol White");

        LeaveApprover saved = leaveApproverRepository.save(LeaveApprover.builder()
                .staff(staff).approver(approver)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(admin).adminDate(LocalDate.of(2023, 12, 1))
                .build());

        leaveApproverRepository.deleteById(saved.getId());
        assertThat(leaveApproverRepository.findById(saved.getId())).isEmpty();
    }
}
