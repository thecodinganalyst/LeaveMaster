package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AustraliaHolidayCalendarInitializer implements ApplicationRunner {
    static final LocalDate START_2026 = LocalDate.of(2026, 1, 1);
    static final LocalDate END_2026 = LocalDate.of(2026, 12, 31);

    private final LeaveCalendarRepository leaveCalendarRepository;

    private record Holiday(int month, int day, String name) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        calendars().forEach(this::reconcileCalendar);
    }

    private void reconcileCalendar(String jurisdictionId, List<Holiday> holidays) {
        String id = "template:" + jurisdictionId + ":2026-01-01_2026-12-31";
        LeaveCalendar calendar = leaveCalendarRepository.findById(id)
                .orElseGet(() -> LeaveCalendar.builder()
                        .id(id)
                        .start(START_2026)
                        .end(END_2026)
                        .tenantId(null)
                        .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                        .jurisdictionId(jurisdictionId)
                        .sourceTemplateId(null)
                        .publicHolidays(new ArrayList<>())
                        .build());

        calendar.setStart(START_2026);
        calendar.setEnd(END_2026);
        calendar.setTenantId(null);
        calendar.setScope(ConfigurationScope.PLATFORM_TEMPLATE);
        calendar.setJurisdictionId(jurisdictionId);
        calendar.setSourceTemplateId(null);
        calendar.setPublicHolidays(holidays.stream()
                .map(holiday -> PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, holiday.month(), holiday.day()))
                        .holidayName(holiday.name())
                        .build())
                .toCollection(ArrayList::new));
        leaveCalendarRepository.save(calendar);
    }

    static Map<String, List<Holiday>> calendars() {
        Map<String, List<Holiday>> calendars = new LinkedHashMap<>();
        calendars.put("AU-ACT", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"), h(3, 9, "Canberra Day"),
                h(4, 3, "Good Friday"), h(4, 4, "Easter Saturday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(4, 27, "Additional public holiday for Anzac Day"),
                h(6, 1, "Reconciliation Day"), h(6, 8, "King's Birthday"), h(10, 5, "Labour Day"),
                h(12, 25, "Christmas Day"), h(12, 26, "Boxing Day"), h(12, 28, "Additional public holiday for Boxing Day")
        ));
        calendars.put("AU-NSW", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"),
                h(4, 3, "Good Friday"), h(4, 4, "Easter Saturday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(4, 27, "Additional public holiday for Anzac Day"),
                h(6, 8, "King's Birthday"), h(10, 5, "Labour Day"),
                h(12, 25, "Christmas Day"), h(12, 26, "Boxing Day"), h(12, 28, "Additional public holiday for Boxing Day")
        ));
        calendars.put("AU-NT", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"),
                h(4, 3, "Good Friday"), h(4, 4, "Easter Saturday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(5, 4, "May Day"), h(6, 8, "King's Birthday"), h(8, 3, "Picnic Day"),
                h(12, 25, "Christmas Day"), h(12, 26, "Boxing Day"), h(12, 28, "Additional public holiday for Boxing Day")
        ));
        calendars.put("AU-QLD", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"),
                h(4, 3, "Good Friday"), h(4, 4, "The day after Good Friday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(5, 4, "Labour Day"), h(10, 5, "King's Birthday"),
                h(12, 25, "Christmas Day"), h(12, 26, "Boxing Day"), h(12, 28, "Additional public holiday for Boxing Day")
        ));
        calendars.put("AU-SA", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"), h(3, 9, "Adelaide Cup Day"),
                h(4, 3, "Good Friday"), h(4, 4, "Easter Saturday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(6, 8, "King's Birthday"), h(10, 5, "Labour Day"),
                h(12, 25, "Christmas Day"), h(12, 26, "Proclamation Day holiday"), h(12, 28, "Additional public holiday for Proclamation Day holiday")
        ));
        calendars.put("AU-TAS", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"), h(3, 9, "Eight Hours Day"),
                h(4, 3, "Good Friday"), h(4, 6, "Easter Monday"), h(4, 25, "Anzac Day"), h(6, 8, "King's Birthday"),
                h(12, 25, "Christmas Day"), h(12, 28, "Boxing Day")
        ));
        calendars.put("AU-VIC", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"), h(3, 9, "Labour Day"),
                h(4, 3, "Good Friday"), h(4, 4, "Saturday before Easter Sunday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(6, 8, "King's Birthday"), h(9, 25, "Friday before the AFL Grand Final"),
                h(12, 25, "Christmas Day"), h(12, 26, "Boxing Day"), h(12, 28, "Additional public holiday for Boxing Day")
        ));
        calendars.put("AU-WA", List.of(
                h(1, 1, "New Year's Day"), h(1, 26, "Australia Day"), h(3, 2, "Labour Day"),
                h(4, 3, "Good Friday"), h(4, 5, "Easter Sunday"), h(4, 6, "Easter Monday"),
                h(4, 25, "Anzac Day"), h(4, 27, "Additional public holiday for Anzac Day"), h(6, 1, "Western Australia Day"),
                h(12, 25, "Christmas Day"), h(12, 26, "Boxing Day"), h(12, 28, "Additional public holiday for Boxing Day")
        ));
        return Map.copyOf(calendars);
    }

    private static Holiday h(int month, int day, String name) {
        return new Holiday(month, day, name);
    }
}
