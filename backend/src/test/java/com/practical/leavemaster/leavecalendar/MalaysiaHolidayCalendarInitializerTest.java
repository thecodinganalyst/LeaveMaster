package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MalaysiaHolidayCalendarInitializerTest {
    private LeaveCalendarRepository repository;
    private MalaysiaHolidayCalendarInitializer initializer;
    private Map<String, LeaveCalendar> stored;

    @BeforeEach
    void setUp() {
        repository = mock(LeaveCalendarRepository.class);
        stored = new LinkedHashMap<>();
        when(repository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.save(any(LeaveCalendar.class)))
                .thenAnswer(invocation -> {
                    LeaveCalendar calendar = invocation.getArgument(0);
                    stored.put(calendar.getId(), calendar);
                    return calendar;
                });
        initializer = new MalaysiaHolidayCalendarInitializer(repository);
    }

    @Test
    void shouldSeedCountryAndAllSixteenChildJurisdictionCalendars() {
        initializer.run(null);

        assertThat(stored).hasSize(17);
        assertThat(stored.values()).allSatisfy(calendar -> {
            assertThat(calendar.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
            assertThat(calendar.getTenantId()).isNull();
            assertThat(calendar.getStart()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(calendar.getEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
            Set<String> keys = calendar.getPublicHolidays().stream()
                    .map(holiday -> holiday.getHolidayDate() + "|" + holiday.getHolidayName())
                    .collect(Collectors.toSet());
            assertThat(keys).hasSameSizeAs(calendar.getPublicHolidays());
        });
    }

    @Test
    void shouldSeparateNationwideAndStateSpecificHolidays() {
        initializer.run(null);

        assertThat(names("MY")).contains("National Day", "Malaysia Day", "Labour Day")
                .doesNotContain("Birthday of the Sultan of Selangor", "Hol Almarhum Sultan Iskandar");
        assertThat(names("MY-SGR")).contains("Birthday of the Sultan of Selangor")
                .doesNotContain("Hol Almarhum Sultan Iskandar");
        assertThat(names("MY-JHR")).contains("Birthday of the Sultan of Johor", "Hol Almarhum Sultan Iskandar")
                .doesNotContain("Birthday of the Sultan of Selangor");
    }

    @Test
    void shouldIncludeOfficialAdditionalAndReplacementDates() {
        initializer.run(null);

        assertThat(dates("MY-SGR", "Additional public holiday for Hari Raya Puasa"))
                .containsExactly(LocalDate.of(2026, 3, 20));
        assertThat(dates("MY-SGR", "Wesak Day replacement holiday"))
                .containsExactly(LocalDate.of(2026, 6, 2));
        assertThat(names("MY-KDH")).doesNotContain("Wesak Day replacement holiday");
    }

    @Test
    void shouldReconcileIdempotently() {
        initializer.run(null);
        Map<String, Integer> sizes = stored.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPublicHolidays().size()));

        initializer.run(null);

        assertThat(stored).hasSize(17);
        stored.forEach((id, calendar) -> assertThat(calendar.getPublicHolidays()).hasSize(sizes.get(id)));
    }

    private Set<String> names(String jurisdictionId) {
        return calendar(jurisdictionId).getPublicHolidays().stream()
                .map(PublicHoliday::getHolidayName)
                .collect(Collectors.toSet());
    }

    private java.util.List<LocalDate> dates(String jurisdictionId, String name) {
        return calendar(jurisdictionId).getPublicHolidays().stream()
                .filter(holiday -> name.equals(holiday.getHolidayName()))
                .map(PublicHoliday::getHolidayDate)
                .toList();
    }

    private LeaveCalendar calendar(String jurisdictionId) {
        return stored.get("template:" + jurisdictionId + ":2026-01-01_2026-12-31");
    }
}
