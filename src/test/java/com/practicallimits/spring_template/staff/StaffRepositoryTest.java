package com.practicallimits.spring_template.staff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StaffRepositoryTest {

    @Autowired
    private StaffRepository staffRepository;

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
}
