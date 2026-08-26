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

class AustraliaHolidayCalendarInitializerTest {
    private LeaveCalendarRepository repository;
    private AustraliaHolidayCalendarInitializer initializer;
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
        initializer = new AustraliaHolidayCalendarInitializer(repository);
    }

    @Test
    void shouldSeedAllEightStateAndTerritoryCalendars() {
        initializer.run(null);

        assertThat(stored).hasSize(8);
        assertThat(stored.values())
                .allSatisfy(calendar -> {
                    assertThat(calendar.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
                    assertThat(calendar.getTenantId()).isNull();
                    assertThat(calendar.getStart()).isEqualTo(LocalDate.of(2026, 1, 1));
                    assertThat(calendar.getEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
                    assertThat(calendar.getJurisdictionId()).startsWith("AU-");
                    Set<String> keys = calendar.getPublicHolidays().stream()
                            .map(holiday -> holiday.getHolidayDate() + "|" + holiday.getHolidayName())
                            .collect(Collectors.toSet());
                    assertThat(keys).hasSameSizeAs(calendar.getPublicHolidays());
                });
    }

    @Test
    void shouldPreserveRepresentativeJurisdictionDifferences() {
        initializer.run(null);

        assertThat(names("AU-ACT")).contains("Canberra Day", "Reconciliation Day");
        assertThat(names("AU-NSW")).doesNotContain("Canberra Day", "Reconciliation Day");
        assertThat(names("AU-QLD")).contains("Labour Day").doesNotContain("Royal Queensland Show");
        assertThat(names("AU-VIC")).contains("Friday before the AFL Grand Final").doesNotContain("Melbourne Cup");
        assertThat(names("AU-WA")).contains("Western Australia Day").doesNotContain("King's Birthday");
        assertThat(names("AU-TAS")).doesNotContain("Royal Hobart Regatta", "Royal Hobart Show", "Recreation Day", "Easter Tuesday");
    }

    @Test
    void shouldOmitPartialDayHolidaysThatCannotBeRepresentedByDateOnlyModel() {
        initializer.run(null);

        assertThat(names("AU-NT")).doesNotContain("Christmas Eve", "New Year's Eve");
        assertThat(names("AU-QLD")).doesNotContain("Christmas Eve");
        assertThat(names("AU-SA")).doesNotContain("Christmas Eve", "New Year's Eve");
    }

    @Test
    void shouldReconcileIdempotently() {
        initializer.run(null);
        Map<String, Integer> sizes = stored.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPublicHolidays().size()));

        initializer.run(null);

        assertThat(stored).hasSize(8);
        stored.forEach((id, calendar) -> assertThat(calendar.getPublicHolidays()).hasSize(sizes.get(id)));
    }

    private Set<String> names(String jurisdictionId) {
        return stored.get("template:" + jurisdictionId + ":2026-01-01_2026-12-31").getPublicHolidays().stream()
                .map(PublicHoliday::getHolidayName)
                .collect(Collectors.toSet());
    }
}
