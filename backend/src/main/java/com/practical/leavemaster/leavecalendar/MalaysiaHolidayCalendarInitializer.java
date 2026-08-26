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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MalaysiaHolidayCalendarInitializer implements ApplicationRunner {
    static final LocalDate START_2026 = LocalDate.of(2026, 1, 1);
    static final LocalDate END_2026 = LocalDate.of(2026, 12, 31);

    private final LeaveCalendarRepository leaveCalendarRepository;

    record Holiday(int month, int day, String name) {}

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
                .collect(Collectors.toCollection(ArrayList::new)));
        leaveCalendarRepository.save(calendar);
    }

    static Map<String, List<Holiday>> calendars() {
        Map<String, List<Holiday>> calendars = new LinkedHashMap<>();

        // Holidays shown for every Malaysian jurisdiction in the official 2026 federal schedule.
        calendars.put("MY", List.of(
                h(2, 17, "Chinese New Year"),
                h(3, 21, "Hari Raya Puasa"), h(3, 22, "Hari Raya Puasa (Second Day)"),
                h(5, 1, "Labour Day"), h(5, 27, "Hari Raya Qurban"), h(5, 31, "Wesak Day"),
                h(6, 1, "Official Birthday of the Yang di-Pertuan Agong"), h(6, 17, "Awal Muharram"),
                h(8, 25, "Maulidur Rasul"), h(8, 31, "National Day"),
                h(9, 16, "Malaysia Day"), h(12, 25, "Christmas Day")
        ));

        calendars.put("MY-JHR", List.of(
                h(2, 1, "Thaipusam"), h(2, 18, "Chinese New Year (Second Day)"),
                h(2, 19, "Awal Ramadan"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(3, 23, "Birthday of the Sultan of Johor"), h(6, 2, "Wesak Day replacement holiday"),
                h(7, 21, "Hol Almarhum Sultan Iskandar"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-KDH", List.of(
                h(1, 17, "Israk and Mikraj"), h(2, 18, "Chinese New Year (Second Day)"),
                h(2, 19, "Awal Ramadan"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(5, 28, "Hari Raya Qurban (Second Day)"), h(6, 21, "Birthday of the Sultan of Kedah"),
                h(11, 8, "Deepavali")
        ));
        calendars.put("MY-KTN", List.of(
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(5, 26, "Hari Arafah"), h(5, 28, "Hari Raya Qurban (Second Day)"),
                h(9, 29, "Birthday of the Sultan of Kelantan"), h(9, 30, "Birthday of the Sultan of Kelantan (Second Day)"),
                h(11, 8, "Deepavali")
        ));
        calendars.put("MY-MLK", List.of(
                h(1, 1, "New Year's Day"), h(2, 18, "Chinese New Year (Second Day)"),
                h(2, 20, "Declaration of Independence Day"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(3, 23, "Hari Raya Puasa (Third Day)"), h(6, 2, "Wesak Day replacement holiday"),
                h(8, 24, "Birthday of the Yang di-Pertua Negeri Melaka"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-NSN", List.of(
                h(1, 1, "New Year's Day"), h(1, 14, "Birthday of the Yang di-Pertuan Besar Negeri Sembilan"),
                h(1, 17, "Israk and Mikraj"), h(2, 1, "Thaipusam"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 20, "Additional public holiday for Hari Raya Puasa"), h(6, 2, "Wesak Day replacement holiday"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-PHG", List.of(
                h(1, 1, "New Year's Day"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(5, 22, "Hol Almarhum Sultan Ahmad Shah"), h(6, 2, "Wesak Day replacement holiday"),
                h(7, 31, "Birthday of the Sultan of Pahang"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-PNG", List.of(
                h(1, 1, "New Year's Day"), h(2, 1, "Thaipusam"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(6, 2, "Wesak Day replacement holiday"), h(7, 7, "World Heritage Site Anniversary"),
                h(7, 11, "Birthday of the Yang di-Pertua Negeri Pulau Pinang"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-PRK", List.of(
                h(1, 1, "New Year's Day"), h(2, 1, "Thaipusam"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(6, 2, "Wesak Day replacement holiday"), h(11, 6, "Birthday of the Sultan of Perak"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-PLS", List.of(
                h(1, 17, "Israk and Mikraj"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(5, 17, "Birthday of the Raja of Perlis"), h(5, 28, "Hari Raya Qurban (Second Day)"),
                h(6, 2, "Wesak Day replacement holiday"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-SBH", List.of(
                h(1, 1, "New Year's Day"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 30, "Birthday of the Yang di-Pertua Negeri Sabah"), h(4, 3, "Good Friday"),
                h(5, 30, "Kaamatan"), h(5, 31, "Kaamatan (Second Day)"),
                h(6, 2, "Wesak Day replacement holiday"), h(11, 8, "Deepavali"),
                h(11, 9, "Deepavali replacement holiday"), h(12, 24, "Christmas Eve")
        ));
        calendars.put("MY-SWK", List.of(
                h(1, 1, "New Year's Day"), h(2, 18, "Chinese New Year (Second Day)"), h(4, 3, "Good Friday"),
                h(6, 1, "Gawai Dayak"), h(6, 2, "Gawai Dayak (Second Day)"),
                h(7, 22, "Sarawak Day"), h(10, 10, "Birthday of the Yang di-Pertua Negeri Sarawak")
        ));
        calendars.put("MY-SGR", List.of(
                h(1, 1, "New Year's Day"), h(2, 1, "Thaipusam"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(6, 2, "Wesak Day replacement holiday"), h(11, 8, "Deepavali"),
                h(11, 9, "Deepavali replacement holiday"), h(12, 11, "Birthday of the Sultan of Selangor")
        ));
        calendars.put("MY-TRG", List.of(
                h(1, 17, "Israk and Mikraj"), h(3, 4, "Anniversary of the Installation of the Sultan of Terengganu"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(4, 26, "Birthday of the Sultan of Terengganu"), h(5, 26, "Hari Arafah"),
                h(5, 28, "Hari Raya Qurban (Second Day)"), h(11, 8, "Deepavali")
        ));
        calendars.put("MY-KUL", List.of(
                h(1, 1, "New Year's Day"), h(2, 1, "Federal Territory Day"), h(2, 1, "Thaipusam"),
                h(2, 18, "Chinese New Year (Second Day)"), h(3, 7, "Nuzul Al-Quran"),
                h(3, 20, "Additional public holiday for Hari Raya Puasa"), h(6, 2, "Wesak Day replacement holiday"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-LBN", List.of(
                h(1, 1, "New Year's Day"), h(2, 1, "Federal Territory Day"), h(2, 18, "Chinese New Year (Second Day)"),
                h(3, 7, "Nuzul Al-Quran"), h(3, 20, "Additional public holiday for Hari Raya Puasa"),
                h(5, 30, "Kaamatan"), h(5, 31, "Kaamatan (Second Day)"), h(6, 2, "Wesak Day replacement holiday"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));
        calendars.put("MY-PJY", List.of(
                h(1, 1, "New Year's Day"), h(2, 1, "Federal Territory Day"), h(2, 1, "Thaipusam"),
                h(2, 18, "Chinese New Year (Second Day)"), h(3, 7, "Nuzul Al-Quran"),
                h(3, 20, "Additional public holiday for Hari Raya Puasa"), h(6, 2, "Wesak Day replacement holiday"),
                h(11, 8, "Deepavali"), h(11, 9, "Deepavali replacement holiday")
        ));

        return Map.copyOf(calendars);
    }

    private static Holiday h(int month, int day, String name) {
        return new Holiday(month, day, name);
    }
}
