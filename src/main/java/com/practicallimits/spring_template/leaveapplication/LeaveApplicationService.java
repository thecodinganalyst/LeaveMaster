package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.email.EmailService;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendar;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendarNotFoundException;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendarService;
import com.practicallimits.spring_template.leavecalendar.PublicHoliday;
import com.practicallimits.spring_template.leaveentitlement.LeaveEntitlement;
import com.practicallimits.spring_template.leaveapprover.LeaveApprover;
import com.practicallimits.spring_template.leaveapprover.LeaveApproverRepository;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeNotFoundException;
import com.practicallimits.spring_template.leavetype.LeaveTypeRepository;
import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.StaffRepository;
import com.practicallimits.spring_template.staff.WorkScheduleDay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveCalendarService leaveCalendarService;
    private final LeaveApproverRepository leaveApproverRepository;
    private final EmailService emailService;

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

    public List<LeaveApplication> findPendingByApproverId(String approverId) {
        staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        return leaveApplicationRepository.findPendingByApproverId(approverId);
    }

    public List<LeaveApplication> findByStaffId(String staffId, LocalDate date) {
        LeaveCalendar calendar = leaveCalendarService.getCalendarFor(date)
                .orElseThrow(() -> new LeaveCalendarNotFoundException(date.toString()));
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        return leaveApplicationRepository.findByStaffAndLeaveDateBetween(staff, calendar.getStart(), calendar.getEnd());
    }

    public List<LeaveBalance> getLeaveBalances(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));

        List<LeaveStatus> countedStatuses = List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING);
        List<LeaveBalance> balances = new ArrayList<>();

        for (LeaveEntitlement entitlement : staff.getLeaveEntitlements()) {
            LeaveType leaveType = entitlement.getLeaveType();
            List<LeaveApplication> applications = leaveApplicationRepository
                    .findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                            staff, leaveType, entitlement.getFrom(), entitlement.getTo(), countedStatuses);

            BigDecimal used = applications.stream()
                    .map(a -> a.getLeaveDuration() == LeaveDuration.FULL ? BigDecimal.ONE : HALF_DAY)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal balance = entitlement.getEntitlement().subtract(used);
            balances.add(new LeaveBalance(leaveType, entitlement.getEntitlement(), used, balance));
        }

        return balances;
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
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));

        boolean isPast = application.getLeaveDate().isBefore(LocalDate.now());

        if (isPast && application.getStatus() == LeaveStatus.APPROVED) {
            application.setStatus(LeaveStatus.CANCEL_REQUESTED);
            leaveApplicationRepository.save(application);
            notifyApproverOfCancellationRequest(application);
        } else {
            application.setStatus(LeaveStatus.CANCELLED);
            leaveApplicationRepository.save(application);
        }
    }

    public LeaveApplication approve(String id, String approverId) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        validatePendingApproval(application);
        Staff approver = staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        validateApproverCanAction(application, approverId);
        application.setStatus(LeaveStatus.APPROVED);
        application.setApprover(approver);
        application.setApprovalDate(LocalDate.now());
        LeaveApplication updated = leaveApplicationRepository.save(application);
        emailService.sendLeaveApprovalNotification(updated);
        return updated;
    }

    public LeaveApplication reject(String id, String approverId) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        validatePendingApproval(application);
        Staff approver = staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        validateApproverCanAction(application, approverId);
        application.setStatus(LeaveStatus.DENIED);
        application.setApprover(approver);
        application.setApprovalDate(LocalDate.now());
        LeaveApplication updated = leaveApplicationRepository.save(application);
        emailService.sendLeaveRejectionNotification(updated);
        return updated;
    }

    public LeaveApplication approveCancellation(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        if (application.getStatus() != LeaveStatus.CANCEL_REQUESTED) {
            throw new IllegalArgumentException("Leave application is not pending cancellation approval");
        }
        application.setStatus(LeaveStatus.CANCELLED);
        return leaveApplicationRepository.save(application);
    }

    public LeaveApplication rejectCancellation(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        if (application.getStatus() != LeaveStatus.CANCEL_REQUESTED) {
            throw new IllegalArgumentException("Leave application is not pending cancellation approval");
        }
        application.setStatus(LeaveStatus.APPROVED);
        return leaveApplicationRepository.save(application);
    }

    private void validatePendingApproval(LeaveApplication application) {
        if (application.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Leave application is not pending approval");
        }
    }

    private void validateApproverCanAction(LeaveApplication application, String approverId) {
        boolean canApprove = leaveApproverRepository.findActiveApproversForStaff(application.getStaff(), application.getLeaveDate())
                .stream()
                .map(LeaveApprover::getApprover)
                .filter(approver -> approver != null)
                .anyMatch(approver -> approverId.equals(approver.getId()));
        if (!canApprove) {
            throw new IllegalArgumentException("Leave application is not pending for this approver");
        }
    }

    private void notifyApproverOfCancellationRequest(LeaveApplication application) {
        List<LeaveApprover> activeApprovers = leaveApproverRepository
                .findActiveApproversForStaff(application.getStaff(), LocalDate.now());
        for (LeaveApprover leaveApprover : activeApprovers) {
            Staff approver = leaveApprover.getApprover();
            if (approver == null) {
                continue;
            }
            emailService.sendCancellationRequestNotification(application, approver.getEmail());
        }
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
