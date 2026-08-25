package com.practical.leavemaster.mcp;

import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(StaffAssistantReadService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StaffAssistantReadServiceTest {

    @Autowired
    private StaffAssistantReadService readService;

    @Autowired
    private StaffRepository staffRepository;

    @AfterEach
    void cleanUp() {
        staffRepository.deleteAll();
    }

    @Test
    void shouldMapLazyWorkScheduleBeforePersistenceContextCloses() {
        staffRepository.save(Staff.builder()
                .id("LAZY-338")
                .name("Alice")
                .joinDate(LocalDate.of(2026, 8, 15))
                .workSchedule(List.of(
                        WorkScheduleDay.builder()
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .daySchedule(DaySchedule.FULL)
                                .build()))
                .build());

        Staff detached = staffRepository.findById("LAZY-338").orElseThrow();
        assertThat(Hibernate.isInitialized(detached.getWorkSchedule())).isFalse();

        StaffAssistantReadService.StaffResult result = readService.findById("LAZY-338").orElseThrow();

        assertThat(result.id()).isEqualTo("LAZY-338");
        assertThat(result.workSchedule()).singleElement().satisfies(day -> {
            assertThat(day.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(day.daySchedule()).isEqualTo(DaySchedule.FULL);
        });
        assertThat(result.leaveEntitlements()).isEmpty();
    }

    @Test
    void shouldReturnSafeDtosForAllStaffAndEmptyForUnknownId() {
        staffRepository.save(Staff.builder()
                .id("ALL-338")
                .name("Bob")
                .joinDate(LocalDate.of(2025, 1, 1))
                .build());

        assertThat(readService.findAll()).singleElement()
                .extracting(StaffAssistantReadService.StaffResult::id)
                .isEqualTo("ALL-338");
        assertThat(readService.findById("missing-338")).isEmpty();
    }
}
