package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IndonesiaHolidayCalendarInitializer implements ApplicationRunner {
    static final LocalDate START_2026 = LocalDate.of(2026, 1, 1);
    static final LocalDate END_2026 = LocalDate.of(2026, 12, 31);

    private final LeaveCalendarRepository leaveCalendarRepository;

    record Holiday(int month, int day, String name) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String id = "template:ID:2026-01-01_2026-12-31";
        LeaveCalendar calendar = leaveCalendarRepository.findById(id)
                .orElseGet(() -> LeaveCalendar.builder()
                        .id(id)
                        .start(START_2026)
                        .end(END_2026)
                        .tenantId(null)
                        .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                        .jurisdictionId("ID")
                        .sourceTemplateId(null)
                        .publicHolidays(new ArrayList<>())
                        .build());

        calendar.setStart(START_2026);
        calendar.setEnd(END_2026);
        calendar.setTenantId(null);
        calendar.setScope(ConfigurationScope.PLATFORM_TEMPLATE);
        calendar.setJurisdictionId("ID");
        calendar.setSourceTemplateId(null);
        calendar.setPublicHolidays(holidays().stream()
                .map(holiday -> PublicHoliday.builder()
                        .holidayDate(LocalDate.of(2026, holiday.month(), holiday.day()))
                        .holidayName(holiday.name())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new)));
        leaveCalendarRepository.save(calendar);
    }

    static List<Holiday> holidays() {
        // National holidays from the 2026 Joint Ministerial Decree (SKB 3 Menteri).
        // Collective-leave (cuti bersama) dates are deliberately excluded and remain tenant-configured.
        return List.of(
                h(1, 1, "New Year's Day 2026"),
                h(1, 16, "Isra Mikraj of Prophet Muhammad"),
                h(2, 17, "Chinese New Year 2577 Kongzili"),
                h(3, 19, "Nyepi - Saka New Year 1948"),
                h(3, 21, "Eid al-Fitr 1447 H"),
                h(3, 22, "Eid al-Fitr 1447 H (Second Day)"),
                h(4, 3, "Good Friday"),
                h(4, 5, "Easter Sunday"),
                h(5, 1, "International Labour Day"),
                h(5, 14, "Ascension of Jesus Christ"),
                h(5, 27, "Eid al-Adha 1447 H"),
                h(5, 31, "Vesak Day 2570 BE"),
                h(6, 1, "Pancasila Day"),
                h(6, 16, "Islamic New Year 1448 H"),
                h(8, 17, "Independence Day"),
                h(8, 25, "Birthday of Prophet Muhammad"),
                h(12, 25, "Christmas Day")
        );
    }

    private static Holiday h(int month, int day, String name) {
        return new Holiday(month, day, name);
    }
}
