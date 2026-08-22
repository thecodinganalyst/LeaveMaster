package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarNotFoundException;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.EventLeaveEntitlement;
import com.practical.leavemaster.leaveentitlement.EventLeaveEntitlementService;
import com.practical.leavemaster.leaveentitlement.EventLeaveEntitlementStatus;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeNotFoundException;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import com.practical.leavemaster.storage.StorageService;
import com.practical.leavemaster.tenant.TenantActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.UUID;
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
    private final TenantActivityService tenantActivityService;
    private final StorageService storageService;
    private final EventLeaveEntitlementService eventLeaveEntitlementService;

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

    public List<LeaveApplication> findVisibleForStaff(String staffId) {
        staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        return leaveApplicationRepository.findVisibleForStaff(staffId);
    }

    public List<LeaveApplication> findPendingByApproverId(String approverId) {
        staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        return leaveApplicationRepository.findPendingByApproverId(approverId);
    }

    public List<LeaveApplication> findByStaffId(String staffId, LocalDate date) {
        Optional<Staff> staffResult = staffRepository.findById(staffId);
        if (staffResult.isEmpty()) {
            if (leaveCalendarService.getCalendarFor(date).isEmpty()) {
                throw new LeaveCalendarNotFoundException(date.toString());
            }
            throw new StaffNotFoundException(staffId);
        }
        Staff staff = staffResult.get();
        LeaveCalendar calendar = calendarForStaff(staff, date)
                .orElseThrow(() -> new LeaveCalendarNotFoundException(date.toString()));
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
                    .map(this::applicationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal balance = entitlement.getEntitlement().subtract(used);
            balances.add(new LeaveBalance(leaveType, entitlement.getEntitlement(), used, balance));
        }

        return balances;
    }

    @Transactional
    public List<LeaveApplication> apply(LeaveApplicationRequest request, MultipartFile attachment) {
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new StaffNotFoundException(request.getStaffId()));

        if (staff.getTermDate() != null && request.getToDate().isAfter(staff.getTermDate())) {
            throw new IllegalArgumentException("Cannot apply for leave after termination date " + staff.getTermDate());
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new LeaveTypeNotFoundException(request.getLeaveTypeId()));
        if (!Objects.equals(staff.getTenantId(), leaveType.getTenantId())) {
            throw new IllegalArgumentException("Leave type does not belong to the staff tenant");
        }

        LeaveDuration leaveDuration = request.getLeaveDuration() != null ? request.getLeaveDuration() : LeaveDuration.FULL;
        LeaveStatus requestedStatus = request.getStatus() != null ? request.getStatus() : LeaveStatus.DRAFT;

        Map<DayOfWeek, WorkScheduleDay> workScheduleMap = staff.getWorkSchedule().stream()
                .collect(Collectors.toMap(WorkScheduleDay::getDayOfWeek, Function.identity()));
        List<LocalDate> leaveDates = getWorkingDatesInRange(
                workScheduleMap.keySet(), request.getFromDate(), request.getToDate()).stream()
                .filter(date -> calendarForStaff(staff, date)
                        .map(calendar -> !isPublicHoliday(date, calendar))
                        .orElse(true))
                .toList();
        if (leaveDates.isEmpty()) {
            throw new IllegalArgumentException("The selected range does not contain any working leave days");
        }

        Optional<EventLeaveEntitlement> eventEntitlement =
                eventLeaveEntitlementService.prepareForRequest(staff, leaveType, request);
        LeaveStatus status = eventEntitlement
                .filter(entitlement -> entitlement.getStatus() == EventLeaveEntitlementStatus.PENDING_VERIFICATION)
                .map(entitlement -> LeaveStatus.PENDING_VERIFICATION)
                .orElse(requestedStatus);

        BigDecimal requestedAmount = applicationAmount(leaveDuration).multiply(BigDecimal.valueOf(leaveDates.size()));
        if (eventEntitlement.isPresent() && countsAgainstEntitlement(status)) {
            eventLeaveEntitlementService.reserve(
                    eventEntitlement.get(), requestedAmount, leaveDates.getFirst(), leaveDates.getLast());
        }

        List<LeaveApplication> applications = new ArrayList<>();
        String sharedAttachmentKey = null;
        if (attachment != null && !attachment.isEmpty()) {
            try {
                sharedAttachmentKey = storageService.store(UUID.randomUUID().toString(), attachment);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store attachment", e);
            }
        }
        for (LocalDate date : leaveDates) {
            LeaveApplication application = LeaveApplication.builder()
                    .staff(staff)
                    .leaveDate(date)
                    .leaveType(leaveType)
                    .leaveDuration(leaveDuration)
                    .status(status)
                    .attachmentUrl(sharedAttachmentKey)
                    .applicationDate(LocalDate.now())
                    .tenantId(staff.getTenantId())
                    .eventEntitlementId(eventEntitlement.map(EventLeaveEntitlement::getId).orElse(null))
                    .build();
            LeaveApplication saved = leaveApplicationRepository.save(application);
            tenantActivityService.touch(resolveTenantId(saved));
            applications.add(saved);
        }
        return applications;
    }

    public LeaveApplication uploadAttachment(String id, MultipartFile file) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        try {
            String storageKey = storageService.store(id, file);
            application.setAttachmentUrl(storageKey);
            LeaveApplication saved = leaveApplicationRepository.save(application);
            tenantActivityService.touch(resolveTenantId(saved));
            return saved;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment", e);
        }
    }

    public void serveAttachment(String id, jakarta.servlet.http.HttpServletResponse response) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        String storageKey = application.getAttachmentUrl();
        if (storageKey == null || storageKey.isBlank()) {
            try {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND, "No attachment for this leave application");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        try {
            storageService.serve(storageKey, response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serve attachment", e);
        }
    }

    @Transactional
    public LeaveApplication update(String id, LeaveApplication updated) {
        LeaveApplication existing = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        LeaveStatus oldStatus = existing.getStatus();
        LeaveDuration oldDuration = existing.getLeaveDuration();
        LeaveStatus newStatus = updated.getStatus();
        LeaveDuration newDuration = updated.getLeaveDuration();

        reconcileEventReservation(existing, oldStatus, oldDuration, newStatus, newDuration);

        existing.setStatus(newStatus);
        existing.setApprover(updated.getApprover());
        existing.setApprovalDate(updated.getApprovalDate());
        existing.setLeaveDuration(newDuration);
        LeaveApplication saved = leaveApplicationRepository.save(existing);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    @Transactional
    public void delete(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));

        boolean isPast = application.getLeaveDate().isBefore(LocalDate.now());

        if (isPast && application.getStatus() == LeaveStatus.APPROVED) {
            application.setStatus(LeaveStatus.CANCEL_REQUESTED);
            LeaveApplication saved = leaveApplicationRepository.save(application);
            tenantActivityService.touch(resolveTenantId(saved));
            notifyApproverOfCancellationRequest(application);
        } else {
            if (countsAgainstEntitlement(application.getStatus())) {
                releaseEventReservation(application);
            }
            application.setStatus(LeaveStatus.CANCELLED);
            LeaveApplication saved = leaveApplicationRepository.save(application);
            tenantActivityService.touch(resolveTenantId(saved));
        }
    }

    public LeaveApplication approve(String id, String approverId) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        validatePendingApproval(application);
        Staff approver = staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        validateApproverAssignment(application, approverId);
        application.setStatus(LeaveStatus.APPROVED);
        application.setApprover(approver);
        application.setApprovalDate(LocalDate.now());
        LeaveApplication updated = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(updated));
        emailService.sendLeaveApprovalNotification(updated);
        return updated;
    }

    @Transactional
    public LeaveApplication reject(String id, String approverId) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        validatePendingApproval(application);
        Staff approver = staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        validateApproverAssignment(application, approverId);
        releaseEventReservation(application);
        application.setStatus(LeaveStatus.DENIED);
        application.setApprover(approver);
        application.setApprovalDate(LocalDate.now());
        LeaveApplication updated = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(updated));
        emailService.sendLeaveRejectionNotification(updated);
        return updated;
    }

    @Transactional
    public LeaveApplication approveCancellation(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        if (application.getStatus() != LeaveStatus.CANCEL_REQUESTED) {
            throw new IllegalArgumentException("Leave application is not pending cancellation approval");
        }
        releaseEventReservation(application);
        application.setStatus(LeaveStatus.CANCELLED);
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    public LeaveApplication rejectCancellation(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        if (application.getStatus() != LeaveStatus.CANCEL_REQUESTED) {
            throw new IllegalArgumentException("Leave application is not pending cancellation approval");
        }
        application.setStatus(LeaveStatus.APPROVED);
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    private void reconcileEventReservation(LeaveApplication application,
                                           LeaveStatus oldStatus, LeaveDuration oldDuration,
                                           LeaveStatus newStatus, LeaveDuration newDuration) {
        if (application.getEventEntitlementId() == null) {
            return;
        }
        boolean oldCounts = countsAgainstEntitlement(oldStatus);
        boolean newCounts = countsAgainstEntitlement(newStatus);
        BigDecimal oldAmount = applicationAmount(oldDuration);
        BigDecimal newAmount = applicationAmount(newDuration);
        if (oldCounts && !newCounts) {
            eventLeaveEntitlementService.release(application.getEventEntitlementId(), oldAmount);
        } else if (!oldCounts && newCounts) {
            eventLeaveEntitlementService.reserve(application.getEventEntitlementId(), newAmount,
                    application.getLeaveDate(), application.getLeaveDate());
        } else if (oldCounts && newCounts) {
            int comparison = newAmount.compareTo(oldAmount);
            if (comparison > 0) {
                eventLeaveEntitlementService.reserve(application.getEventEntitlementId(), newAmount.subtract(oldAmount),
                        application.getLeaveDate(), application.getLeaveDate());
            } else if (comparison < 0) {
                eventLeaveEntitlementService.release(application.getEventEntitlementId(), oldAmount.subtract(newAmount));
            }
        }
    }

    private void releaseEventReservation(LeaveApplication application) {
        eventLeaveEntitlementService.release(application.getEventEntitlementId(), applicationAmount(application));
    }

    private boolean countsAgainstEntitlement(LeaveStatus status) {
        return status == LeaveStatus.PENDING || status == LeaveStatus.APPROVED;
    }

    private BigDecimal applicationAmount(LeaveApplication application) {
        return applicationAmount(application.getLeaveDuration());
    }

    private BigDecimal applicationAmount(LeaveDuration duration) {
        return duration == LeaveDuration.FULL ? BigDecimal.ONE : HALF_DAY;
    }

    private void validatePendingApproval(LeaveApplication application) {
        if (application.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Leave application is not pending approval");
        }
    }

    private void validateApproverAssignment(LeaveApplication application, String approverId) {
        boolean isAssignedApprover = leaveApproverRepository.findActiveApproversForStaff(application.getStaff(), application.getLeaveDate())
                .stream()
                .map(LeaveApprover::getApprover)
                .anyMatch(approver -> approverId.equals(approver.getId()));
        if (!isAssignedApprover) {
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

    private Optional<LeaveCalendar> calendarForStaff(Staff staff, LocalDate date) {
        if (staff.getJurisdictionId() == null || staff.getJurisdictionId().isBlank()) {
            return leaveCalendarService.getCalendarFor(date);
        }
        return leaveCalendarService.getCalendarFor(staff.getJurisdictionId(), date);
    }

    private String resolveTenantId(LeaveApplication application) {
        if (application.getTenantId() != null && !application.getTenantId().isBlank()) {
            return application.getTenantId();
        }
        return application.getStaff() != null ? application.getStaff().getTenantId() : null;
    }

    private boolean isPublicHoliday(LocalDate date, LeaveCalendar calendar) {
        return calendar.getPublicHolidays().stream()
                .anyMatch(publicHoliday -> date.equals(publicHoliday.getHolidayDate()));
    }
}
