package com.practicallimits.spring_template.staff;

import com.practicallimits.spring_template.leaveentitlement.LeaveEntitlement;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StaffRepositoryTest {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void shouldSaveAndFindStaff() {
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .workSchedule("WEEKDAYS")
                .termDate(null)
                .build();

        staffRepository.save(staff);

        Optional<Staff> found = staffRepository.findById("S001");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice Smith");
        assertThat(found.get().getJoinDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(found.get().getWorkSchedule()).isEqualTo("WEEKDAYS");
        assertThat(found.get().getTermDate()).isNull();
    }

    @Test
    void shouldFindAllStaff() {
        staffRepository.save(Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build());
        staffRepository.save(Staff.builder().id("S002").name("Bob Jones").joinDate(LocalDate.of(2023, 6, 1)).workSchedule("WEEKDAYS").build());

        List<Staff> all = staffRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldDeleteStaff() {
        staffRepository.save(Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build());
        staffRepository.deleteById("S001");
        assertThat(staffRepository.findById("S001")).isEmpty();
    }

    @Test
    void shouldSaveStaffWithLeaveEntitlements() {
        LeaveType annual = leaveTypeRepository.save(LeaveType.builder().id("annual").name("Annual Leave").used(true).build());
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .workSchedule("WEEKDAYS")
                .build();
        staff.setLeaveEntitlements(List.of(LeaveEntitlement.builder()
                .staff(staff)
                .leaveType(annual)
                .from(LocalDate.of(2023, 1, 1))
                .to(LocalDate.of(2023, 12, 31))
                .entitlement(new BigDecimal("20.00"))
                .build()));

        staffRepository.save(staff);

        Optional<Staff> found = staffRepository.findById("S001");
        assertThat(found).isPresent();
        assertThat(found.get().getLeaveEntitlements()).hasSize(1);
        assertThat(found.get().getLeaveEntitlements().getFirst().getLeaveType().getId()).isEqualTo("annual");
        assertThat(found.get().getLeaveEntitlements().getFirst().getEntitlement()).isEqualByComparingTo("20.00");
    }
}
