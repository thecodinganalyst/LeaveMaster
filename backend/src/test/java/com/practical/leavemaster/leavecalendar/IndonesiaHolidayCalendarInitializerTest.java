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

class IndonesiaHolidayCalendarInitializerTest {
    private LeaveCalendarRepository repository;
    private IndonesiaHolidayCalendarInitializer initializer;
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
        initializer = new IndonesiaHolidayCalendarInitializer(repository);
    }

    @Test
    void shouldSeedSeventeenNationalHolidayDaysAndExcludeCollectiveLeave() {
        initializer.run(null);

        LeaveCalendar calendar = stored.get("template:ID:2026-01-01_2026-12-31");
        assertThat(calendar).isNotNull();
        assertThat(calendar.getScope()).isEqualTo(ConfigurationScope.PLATFORM_TEMPLATE);
        assertThat(calendar.getTenantId()).isNull();
        assertThat(calendar.getJurisdictionId()).isEqualTo("ID");
        assertThat(calendar.getStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(calendar.getEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(calendar.getPublicHolidays()).hasSize(17);

        Set<LocalDate> dates = calendar.getPublicHolidays().stream()
                .map(PublicHoliday::getHolidayDate)
                .collect(Collectors.toSet());
        assertThat(dates).hasSize(17)
                .contains(LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 22), LocalDate.of(2026, 8, 17))
                .doesNotContain(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 24));
    }

    @Test
    void shouldReconcileIdempotently() {
        initializer.run(null);
        initializer.run(null);

        assertThat(stored).hasSize(1);
        assertThat(stored.get("template:ID:2026-01-01_2026-12-31").getPublicHolidays()).hasSize(17);
    }
}
