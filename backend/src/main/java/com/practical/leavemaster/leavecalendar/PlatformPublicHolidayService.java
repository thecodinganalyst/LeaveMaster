package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlatformPublicHolidayService {

    private final LeaveCalendarRepository leaveCalendarRepository;

    public List<PlatformPublicHoliday> findAll(String jurisdictionId, Integer year) {
        return leaveCalendarRepository.findAll().stream()
                .filter(this::isPlatformTemplate)
                .filter(calendar -> jurisdictionId == null || jurisdictionId.isBlank()
                        || jurisdictionId.trim().equals(calendar.getJurisdictionId()))
                .flatMap(calendar -> calendar.getPublicHolidays().stream()
                        .filter(holiday -> year == null || holiday.getHolidayDate().getYear() == year)
                        .map(holiday -> toResource(calendar, holiday)))
                .sorted(Comparator.comparing(PlatformPublicHoliday::holidayDate)
                        .thenComparing(PlatformPublicHoliday::holidayName))
                .toList();
    }

    public PlatformPublicHoliday findById(String id) {
        HolidayKey key = decodeId(id);
        LeaveCalendar calendar = platformCalendar(key.calendarId());
        PublicHoliday holiday = findHoliday(calendar, key);
        return toResource(calendar, holiday);
    }

    @Transactional
    public PlatformPublicHoliday create(PlatformPublicHolidayRequest request) {
        validate(request);
        LeaveCalendar calendar = findOrCreateCalendar(request.jurisdictionId().trim(), request.holidayDate().getYear());
        ensureUnique(calendar, request.holidayDate(), request.holidayName(), null);
        PublicHoliday holiday = PublicHoliday.builder()
                .holidayDate(request.holidayDate())
                .holidayName(request.holidayName().trim())
                .build();
        calendar.getPublicHolidays().add(holiday);
        leaveCalendarRepository.save(calendar);
        return toResource(calendar, holiday);
    }

    @Transactional
    public PlatformPublicHoliday update(String id, PlatformPublicHolidayRequest request) {
        validate(request);
        HolidayKey key = decodeId(id);
        LeaveCalendar existingCalendar = platformCalendar(key.calendarId());
        PublicHoliday existingHoliday = findHoliday(existingCalendar, key);
        String jurisdictionId = request.jurisdictionId().trim();
        int year = request.holidayDate().getYear();
        LeaveCalendar targetCalendar = existingCalendar.getJurisdictionId().equals(jurisdictionId)
                && request.holidayDate().getYear() >= existingCalendar.getStart().getYear()
                && request.holidayDate().getYear() <= existingCalendar.getEnd().getYear()
                ? existingCalendar
                : findOrCreateCalendar(jurisdictionId, year);

        existingCalendar.getPublicHolidays().remove(existingHoliday);
        ensureUnique(targetCalendar, request.holidayDate(), request.holidayName(), null);
        PublicHoliday replacement = PublicHoliday.builder()
                .holidayDate(request.holidayDate())
                .holidayName(request.holidayName().trim())
                .build();
        targetCalendar.getPublicHolidays().add(replacement);
        leaveCalendarRepository.save(existingCalendar);
        if (!existingCalendar.getId().equals(targetCalendar.getId())) {
            leaveCalendarRepository.save(targetCalendar);
        }
        return toResource(targetCalendar, replacement);
    }

    @Transactional
    public void delete(String id) {
        HolidayKey key = decodeId(id);
        LeaveCalendar calendar = platformCalendar(key.calendarId());
        PublicHoliday holiday = findHoliday(calendar, key);
        calendar.getPublicHolidays().remove(holiday);
        leaveCalendarRepository.save(calendar);
    }

    private LeaveCalendar findOrCreateCalendar(String jurisdictionId, int year) {
        LocalDate date = LocalDate.of(year, 1, 1);
        return leaveCalendarRepository.findAllByScopeAndJurisdictionId(ConfigurationScope.PLATFORM_TEMPLATE, jurisdictionId).stream()
                .filter(this::isPlatformTemplate)
                .filter(calendar -> !date.isBefore(calendar.getStart()) && !date.isAfter(calendar.getEnd()))
                .findFirst()
                .orElseGet(() -> leaveCalendarRepository.save(LeaveCalendar.builder()
                        .id("template:" + jurisdictionId + ":" + year + "-01-01_" + year + "-12-31")
                        .start(LocalDate.of(year, 1, 1))
                        .end(LocalDate.of(year, 12, 31))
                        .tenantId(null)
                        .scope(ConfigurationScope.PLATFORM_TEMPLATE)
                        .jurisdictionId(jurisdictionId)
                        .build()));
    }

    private LeaveCalendar platformCalendar(String calendarId) {
        return leaveCalendarRepository.findById(calendarId)
                .filter(this::isPlatformTemplate)
                .orElseThrow(() -> new IllegalArgumentException("Platform public holiday not found"));
    }

    private PublicHoliday findHoliday(LeaveCalendar calendar, HolidayKey key) {
        return calendar.getPublicHolidays().stream()
                .filter(holiday -> holiday.getHolidayDate().equals(key.date()))
                .filter(holiday -> holiday.getHolidayName().equals(key.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Platform public holiday not found"));
    }

    private void ensureUnique(LeaveCalendar calendar, LocalDate date, String name, PublicHoliday ignored) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        boolean duplicate = calendar.getPublicHolidays().stream()
                .filter(holiday -> holiday != ignored)
                .anyMatch(holiday -> holiday.getHolidayDate().equals(date)
                        && holiday.getHolidayName().trim().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate public holiday for date and name");
        }
    }

    private void validate(PlatformPublicHolidayRequest request) {
        if (request == null || request.jurisdictionId() == null || request.jurisdictionId().isBlank()) {
            throw new IllegalArgumentException("jurisdictionId is required");
        }
        if (request.holidayDate() == null) {
            throw new IllegalArgumentException("holidayDate is required");
        }
        if (request.holidayName() == null || request.holidayName().isBlank()) {
            throw new IllegalArgumentException("holidayName is required");
        }
    }

    private boolean isPlatformTemplate(LeaveCalendar calendar) {
        return calendar.getScope() == ConfigurationScope.PLATFORM_TEMPLATE
                && calendar.getTenantId() == null
                && calendar.getJurisdictionId() != null;
    }

    private PlatformPublicHoliday toResource(LeaveCalendar calendar, PublicHoliday holiday) {
        return new PlatformPublicHoliday(
                encodeId(calendar.getId(), holiday.getHolidayDate(), holiday.getHolidayName()),
                calendar.getId(),
                calendar.getJurisdictionId(),
                holiday.getHolidayDate().getYear(),
                holiday.getHolidayDate(),
                holiday.getHolidayName());
    }

    private String encodeId(String calendarId, LocalDate date, String name) {
        String raw = calendarId + "\n" + date + "\n" + name;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private HolidayKey decodeId(String id) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(id), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\n", 3);
            if (parts.length != 3) throw new IllegalArgumentException();
            return new HolidayKey(parts[0], LocalDate.parse(parts[1]), parts[2]);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid platform public holiday id");
        }
    }

    private record HolidayKey(String calendarId, LocalDate date, String name) {
    }
}
