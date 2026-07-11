package com.practicallimits.spring_template.leavecalendar;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveCalendarService {

    private final LeaveCalendarRepository leaveCalendarRepository;

    public List<LeaveCalendar> findAll() {
        return leaveCalendarRepository.findAllByOrderByStartAsc();
    }

    public Optional<LeaveCalendar> findById(String id) {
        return leaveCalendarRepository.findById(id);
    }

    public LeaveCalendar create(LeaveCalendar leaveCalendar) {
        validate(leaveCalendar);

        LeaveCalendar normalized = copyOf(leaveCalendar);
        if (normalized.getId() == null || normalized.getId().isBlank()) {
            normalized.setId(defaultId(normalized.getStart(), normalized.getEnd()));
        }

        if (leaveCalendarRepository.existsById(normalized.getId())) {
            throw new LeaveCalendarConflictException("Leave calendar already exists: " + normalized.getId());
        }

        if (leaveCalendarRepository.existsByStartLessThanEqualAndEndGreaterThanEqual(normalized.getEnd(), normalized.getStart())) {
            throw new LeaveCalendarConflictException("Leave calendar overlaps an existing calendar");
        }

        return leaveCalendarRepository.save(normalized);
    }

    public Optional<LeaveCalendar> getCalendarFor(LocalDate date) {
        Optional<LeaveCalendar> existing = leaveCalendarRepository.findByStartLessThanEqualAndEndGreaterThanEqual(date, date);
        if (existing.isPresent()) {
            return existing;
        }

        Optional<LeaveCalendar> latest = leaveCalendarRepository.findTopByOrderByEndDesc();
        if (latest.isEmpty() || !date.isAfter(latest.get().getEnd())) {
            return Optional.empty();
        }

        LeaveCalendar calendar = latest.get();
        while (date.isAfter(calendar.getEnd())) {
            calendar = leaveCalendarRepository.save(nextCalendarFrom(calendar));
        }

        return Optional.of(calendar);
    }

    private void validate(LeaveCalendar leaveCalendar) {
        if (leaveCalendar.getStart() == null || leaveCalendar.getEnd() == null) {
            throw new IllegalArgumentException("Leave calendar start and end dates are required");
        }

        if (leaveCalendar.getStart().isAfter(leaveCalendar.getEnd())) {
            throw new IllegalArgumentException("Leave calendar start date must be on or before end date");
        }

        List<PublicHoliday> publicHolidays = leaveCalendar.getPublicHolidays() == null ? List.of() : leaveCalendar.getPublicHolidays();
        for (PublicHoliday publicHoliday : publicHolidays) {
            if (publicHoliday.getHolidayDate() == null || publicHoliday.getHolidayName() == null || publicHoliday.getHolidayName().isBlank()) {
                throw new IllegalArgumentException("Public holidays require both holidayDate and holidayName");
            }
            if (publicHoliday.getHolidayDate().isBefore(leaveCalendar.getStart())
                    || publicHoliday.getHolidayDate().isAfter(leaveCalendar.getEnd())) {
                throw new IllegalArgumentException("Public holiday must be within the leave calendar range");
            }
        }
    }

    private LeaveCalendar nextCalendarFrom(LeaveCalendar current) {
        LocalDate nextStart = current.getStart().plusYears(1);
        LocalDate nextEnd = current.getEnd().plusYears(1);

        List<PublicHoliday> nextPublicHolidays = current.getPublicHolidays().stream()
                .map(publicHoliday -> PublicHoliday.builder()
                        .holidayDate(publicHoliday.getHolidayDate().plusYears(1))
                        .holidayName(publicHoliday.getHolidayName())
                        .build())
                .toList();

        return LeaveCalendar.builder()
                .id(defaultId(nextStart, nextEnd))
                .start(nextStart)
                .end(nextEnd)
                .publicHolidays(nextPublicHolidays)
                .build();
    }

    private LeaveCalendar copyOf(LeaveCalendar leaveCalendar) {
        List<PublicHoliday> publicHolidays = leaveCalendar.getPublicHolidays() == null
                ? new ArrayList<>()
                : leaveCalendar.getPublicHolidays().stream()
                .map(publicHoliday -> PublicHoliday.builder()
                        .holidayDate(publicHoliday.getHolidayDate())
                        .holidayName(publicHoliday.getHolidayName())
                        .build())
                .toList();

        return LeaveCalendar.builder()
                .id(leaveCalendar.getId())
                .start(leaveCalendar.getStart())
                .end(leaveCalendar.getEnd())
                .publicHolidays(publicHolidays)
                .build();
    }

    private String defaultId(LocalDate start, LocalDate end) {
        return start + "_" + end;
    }
}
