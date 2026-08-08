package com.practical.leavemaster.leavecalendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LeaveCalendarRepositoryTest {

    @Autowired
    private LeaveCalendarRepository leaveCalendarRepository;

    @Test
    void shouldSaveAndFindLeaveCalendarWithPublicHolidays() {
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("fy2026")
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .publicHolidays(List.of(
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 1, 1)).holidayName("New Year").build(),
                        PublicHoliday.builder().holidayDate(LocalDate.of(2026, 12, 25)).holidayName("Christmas").build()
                ))
                .build();

        leaveCalendarRepository.save(leaveCalendar);

        Optional<LeaveCalendar> found = leaveCalendarRepository.findById("fy2026");
        assertThat(found).isPresent();
        assertThat(found.get().getStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(found.get().getPublicHolidays()).hasSize(2);
        assertThat(found.get().getPublicHolidays())
                .extracting(PublicHoliday::getHolidayName)
                .containsExactlyInAnyOrder("New Year", "Christmas");
    }
}
