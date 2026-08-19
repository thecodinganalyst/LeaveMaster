package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SingaporeHolidayCalendarSeedTest {

    @Autowired
    private LeaveCalendarRepository leaveCalendarRepository;

    @Test
    void seedsSingapore2026And2027PlatformTemplates() {
        List<LeaveCalendar> templates = leaveCalendarRepository
                .findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, "SG");

        assertThat(templates)
                .extracting(LeaveCalendar::getId)
                .contains("template:SG:2026-01-01_2026-12-31", "template:SG:2027-01-01_2027-12-31");

        assertThat(templates).filteredOn(calendar -> calendar.getId().startsWith("template:SG:202"))
                .allSatisfy(calendar -> {
                    assertThat(calendar.getTenantId()).isNull();
                    assertThat(calendar.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
                    assertThat(calendar.getJurisdictionId()).isEqualTo("SG");
                    assertThat(calendar.getSourceTemplateId()).isNull();
                });
    }

    @Test
    void seedsSingapore2026GazettedHolidayDates() {
        LeaveCalendar calendar = leaveCalendarRepository
                .findById("template:SG:2026-01-01_2026-12-31")
                .orElseThrow();

        assertThat(calendar.getPublicHolidays()).hasSize(11);
        assertThat(toHolidayMap(calendar)).containsAllEntriesOf(Map.ofEntries(
                Map.entry(LocalDate.of(2026, 1, 1), "New Year's Day"),
                Map.entry(LocalDate.of(2026, 2, 17), "Chinese New Year"),
                Map.entry(LocalDate.of(2026, 2, 18), "Chinese New Year"),
                Map.entry(LocalDate.of(2026, 3, 21), "Hari Raya Puasa"),
                Map.entry(LocalDate.of(2026, 4, 3), "Good Friday"),
                Map.entry(LocalDate.of(2026, 5, 1), "Labour Day"),
                Map.entry(LocalDate.of(2026, 5, 27), "Hari Raya Haji"),
                Map.entry(LocalDate.of(2026, 5, 31), "Vesak Day"),
                Map.entry(LocalDate.of(2026, 8, 9), "National Day"),
                Map.entry(LocalDate.of(2026, 11, 8), "Deepavali"),
                Map.entry(LocalDate.of(2026, 12, 25), "Christmas Day")
        ));
    }

    @Test
    void seedsSingapore2027GazettedHolidayDates() {
        LeaveCalendar calendar = leaveCalendarRepository
                .findById("template:SG:2027-01-01_2027-12-31")
                .orElseThrow();

        assertThat(calendar.getPublicHolidays()).hasSize(11);
        assertThat(toHolidayMap(calendar)).containsAllEntriesOf(Map.ofEntries(
                Map.entry(LocalDate.of(2027, 1, 1), "New Year's Day"),
                Map.entry(LocalDate.of(2027, 2, 6), "Chinese New Year"),
                Map.entry(LocalDate.of(2027, 2, 7), "Chinese New Year"),
                Map.entry(LocalDate.of(2027, 3, 10), "Hari Raya Puasa"),
                Map.entry(LocalDate.of(2027, 3, 26), "Good Friday"),
                Map.entry(LocalDate.of(2027, 5, 1), "Labour Day"),
                Map.entry(LocalDate.of(2027, 5, 17), "Hari Raya Haji"),
                Map.entry(LocalDate.of(2027, 5, 20), "Vesak Day"),
                Map.entry(LocalDate.of(2027, 8, 9), "National Day"),
                Map.entry(LocalDate.of(2027, 10, 28), "Deepavali"),
                Map.entry(LocalDate.of(2027, 12, 25), "Christmas Day")
        ));
    }

    private Map<LocalDate, String> toHolidayMap(LeaveCalendar calendar) {
        return calendar.getPublicHolidays().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PublicHoliday::getHolidayDate,
                        PublicHoliday::getHolidayName));
    }
}
