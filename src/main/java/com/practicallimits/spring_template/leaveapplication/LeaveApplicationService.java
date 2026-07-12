package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.leavecalendar.LeaveCalendar;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendarService;
import com.practicallimits.spring_template.leavecalendar.PublicHoliday;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeNotFoundException;
import com.practicallimits.spring_template.leavetype.LeaveTypeRepository;
import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.StaffRepository;
import com.practicallimits.spring_template.staff.WorkScheduleDay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveCalendarService leaveCalendarService;

    public List<LeaveApplication> findAll() {
        return leaveApplicationRepository.findAll();
    }

    public Optional<LeaveApplication> findById(String id) {
        return leaveApplicationRepository.findById(id);
    }

    public List<LeaveApplication> findByStaffId(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        return leaveApplicationRepository.findByStaff(staff);
    }

    public List<LeaveApplication> apply(LeaveApplicationRequest request) {
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new LeaveTypeNotFoundException(request.getLeaveTypeId()));

        LeaveDuration leaveDuration = request.getLeaveDuration() != null ? request.getLeaveDuration() : LeaveDuration.FULL;
        LeaveStatus status = request.getStatus() != null ? request.getStatus() : LeaveStatus.DRAFT;

        Map<DayOfWeek, WorkScheduleDay> workScheduleMap = staff.getWorkSchedule().stream()
                .collect(Collectors.toMap(WorkScheduleDay::getDayOfWeek, Function.identity()));

        List<LocalDate> leaveDates = getWorkingDatesInRange(
                workScheduleMap.keySet(), request.getFromDate(), request.getToDate());

        List<LeaveApplication> applications = new ArrayList<>();
        for (LocalDate date : leaveDates) {
            Optional<LeaveCalendar> calendar = leaveCalendarService.getCalendarFor(date);
            if (calendar.isPresent() && isPublicHoliday(date, calendar.get())) {
                continue;
            }
            LeaveApplication application = LeaveApplication.builder()
                    .staff(staff)
                    .leaveDate(date)
                    .leaveType(leaveType)
                    .leaveDuration(leaveDuration)
                    .status(status)
                    .attachment(request.getAttachment())
                    .applicationDate(LocalDate.now())
                    .build();
            applications.add(leaveApplicationRepository.save(application));
        }
        return applications;
    }

    public LeaveApplication update(String id, LeaveApplication updated) {
        LeaveApplication existing = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        existing.setStatus(updated.getStatus());
        existing.setApprover(updated.getApprover());
        existing.setApprovalDate(updated.getApprovalDate());
        existing.setLeaveDuration(updated.getLeaveDuration());
        existing.setAttachment(updated.getAttachment());
        return leaveApplicationRepository.save(existing);
    }

    public void delete(String id) {
        leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        leaveApplicationRepository.deleteById(id);
    }

    private List<LocalDate> getWorkingDatesInRange(Set<DayOfWeek> workDays, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            if (workDays.contains(date.getDayOfWeek())) {
                dates.add(date);
            }
            date = date.plusDays(1);
        }
        return dates;
    }

    private boolean isPublicHoliday(LocalDate date, LeaveCalendar calendar) {
        return calendar.getPublicHolidays().stream()
                .map(PublicHoliday::getHolidayDate)
                .anyMatch(date::equals);
    }
}
