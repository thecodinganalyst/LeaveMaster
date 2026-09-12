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
import java.util.UUID;
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
            BigDecimal used = applications.stream().map(this::applicationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            balances.add(new LeaveBalance(leaveType, entitlement.getEntitlement(), used,
                    entitlement.getEntitlement().subtract(used)));
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
        if (request.getFromDate().isBefore(staff.getJoinDate())) {
            throw new IllegalArgumentException("Cannot apply for leave before employment start date " + staff.getJoinDate());
        }
        if (staff.getTermDate() != null && request.getToDate().isAfter(staff.getTermDate())) {
            throw new IllegalArgumentException("Cannot apply for leave after termination date " + staff.getTermDate());
        }
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new LeaveTypeNotFoundException(request.getLeaveTypeId()));
        if (leaveType.getTenantId() != null && !Objects.equals(staff.getTenantId(), leaveType.getTenantId())) {
            throw new IllegalArgumentException("Leave type does not belong to the staff tenant");
        }

        LeaveDuration leaveDuration = request.getLeaveDuration() != null ? request.getLeaveDuration() : LeaveDuration.FULL;
        LeaveStatus requestedStatus = request.getStatus() != null ? request.getStatus() : LeaveStatus.DRAFT;
        Map<DayOfWeek, WorkScheduleDay> workScheduleMap = staff.getWorkSchedule().stream()
                .collect(Collectors.toMap(WorkScheduleDay::getDayOfWeek, Function.identity()));
        List<LocalDate> leaveDates = getWorkingDatesInRange(workScheduleMap.keySet(), request.getFromDate(), request.getToDate())
                .stream()
                .filter(date -> calendarForStaff(staff, date)
                        .map(calendar -> !isPublicHoliday(date, calendar)).orElse(true))
                .toList();
        if (leaveDates.isEmpty()) {
            return List.of();
        }

        Optional<EventLeaveEntitlement> eventEntitlement = eventLeaveEntitlementService == null
                ? Optional.empty()
                : eventLeaveEntitlementService.prepareForRequest(staff, leaveType, request);
        LeaveStatus status = eventEntitlement
                .filter(entitlement -> entitlement.getStatus() == EventLeaveEntitlementStatus.PENDING_VERIFICATION)
                .map(entitlement -> LeaveStatus.PENDING_VERIFICATION)
                .orElse(requestedStatus);
        BigDecimal requestedAmount = applicationAmount(leaveDuration).multiply(BigDecimal.valueOf(leaveDates.size()));
        if (eventLeaveEntitlementService != null && eventEntitlement.isPresent() && countsAgainstEntitlement(status)) {
            eventLeaveEntitlementService.reserve(eventEntitlement.get(), requestedAmount,
                    leaveDates.getFirst(), leaveDates.getLast());
        }

        String sharedAttachmentKey = null;
        if (attachment != null && !attachment.isEmpty()) {
            try {
                sharedAttachmentKey = storageService.store(UUID.randomUUID().toString(), attachment);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store attachment", e);
            }
        }
        List<LeaveApplication> applications = new ArrayList<>();
        for (LocalDate date : leaveDates) {
            LeaveApplication saved = leaveApplicationRepository.save(LeaveApplication.builder()
                    .staff(staff)
                    .leaveDate(date)
                    .leaveType(leaveType)
                    .leaveDuration(leaveDuration)
                    .status(status)
                    .attachmentUrl(sharedAttachmentKey)
                    .applicationDate(LocalDate.now())
                    .tenantId(staff.getTenantId())
                    .eventEntitlementId(eventEntitlement.map(EventLeaveEntitlement::getId).orElse(null))
                    .build());
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
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND,
                        "No attachment for this leave application");
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
        reconcileEventReservation(existing, existing.getStatus(), existing.getLeaveDuration(),
                updated.getStatus(), updated.getLeaveDuration());
        existing.setStatus(updated.getStatus());
        existing.setApprover(updated.getApprover());
        existing.setApprovalDate(updated.getApprovalDate());
        existing.setLeaveDuration(updated.getLeaveDuration());
        LeaveApplication saved = leaveApplicationRepository.save(existing);
        tenantActivityService.touch(resolveTenantId(saved));
        return saved;
    }

    @Transactional
    public void delete(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        boolean isPast = application.getLeaveDate().isBefore(LocalDate.now());
        if (!isPast || application.getStatus() != LeaveStatus.APPROVED) {
            LeaveStatus oldStatus = application.getStatus();
            application.setStatus(LeaveStatus.CANCELLED);
            reconcileEventReservation(application, oldStatus, application.getLeaveDuration(),
                    LeaveStatus.CANCELLED, application.getLeaveDuration());
            LeaveApplication saved = leaveApplicationRepository.save(application);
            tenantActivityService.touch(resolveTenantId(saved));
            return;
        }

        application.setStatus(LeaveStatus.CANCEL_REQUESTED);
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        List<LeaveApprover> approvers = leaveApproverRepository.findActiveApproversForStaff(
                application.getStaff(), application.getLeaveDate());
        approvers.forEach(approver -> {
            if (approver.getApprover() != null && approver.getApprover().getEmail() != null) {
                emailService.sendCancellationRequestNotification(application, approver.getApprover().getEmail());
            }
        });
    }

    @Transactional
    public LeaveApplication approve(String id, String approverId) {
        LeaveApplication application = requirePendingApplication(id);
        Staff approver = requireAssignedApprover(application, approverId);
        application.setStatus(LeaveStatus.APPROVED);
        application.setApprover(approver);
        application.setApprovalDate(LocalDate.now());
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        emailService.sendLeaveApprovalNotification(saved);
        return saved;
    }

    @Transactional
    public LeaveApplication reject(String id, String approverId) {
        LeaveApplication application = requirePendingApplication(id);
        Staff approver = requireAssignedApprover(application, approverId);
        LeaveStatus oldStatus = application.getStatus();
        application.setStatus(LeaveStatus.DENIED);
        application.setApprover(approver);
        application.setApprovalDate(LocalDate.now());
        reconcileEventReservation(application, oldStatus, application.getLeaveDuration(),
                LeaveStatus.DENIED, application.getLeaveDuration());
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        emailService.sendLeaveRejectionNotification(saved);
        return saved;
    }

    public LeaveApplication approveCancellation(String id) {
        LeaveApplication application = requireCancelRequested(id);
        LeaveStatus oldStatus = application.getStatus();
        application.setStatus(LeaveStatus.CANCELLED);
        reconcileEventReservation(application, oldStatus, application.getLeaveDuration(),
                LeaveStatus.CANCELLED, application.getLeaveDuration());
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        emailService.sendCancellationApprovalNotification(saved);
        return saved;
    }

    public LeaveApplication rejectCancellation(String id) {
        LeaveApplication application = requireCancelRequested(id);
        application.setStatus(LeaveStatus.APPROVED);
        LeaveApplication saved = leaveApplicationRepository.save(application);
        tenantActivityService.touch(resolveTenantId(saved));
        emailService.sendCancellationRejectionNotification(saved);
        return saved;
    }

    private LeaveApplication requirePendingApplication(String id) {
        LeaveApplication application = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        if (application.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Leave application is not pending approval");
        }
        return application;
    }

    private Staff requireAssignedApprover(LeaveApplication application, String approverId) {
        Staff approver = staffRepository.findById(approverId)
                .orElseThrow(() -> new StaffNotFoundException(approverId));
        boolean assigned = leaveApproverRepository.findActiveApproversForStaff(
                        application.getStaff(), application.getLeaveDate()).stream()
                .map(LeaveApprover::getApprover)
                .filter(Objects::nonNull)
                .anyMatch(staff -> Objects.equals(staff.getId(), approverId));
        if (!assigned) {
            throw new IllegalArgumentException("Leave application is not pending for this approver");
        }
        return approver;
    }

    private BigDecimal applicationAmount(LeaveApplication application) {
        return applicationAmount(application.getLeaveDuration());
    }

    private BigDecimal applicationAmount(LeaveDuration duration) {
        return duration == LeaveDuration.HALF ? HALF_DAY : BigDecimal.ONE;
    }

    private boolean countsAgainstEntitlement(LeaveStatus status) {
        return status == LeaveStatus.PENDING || status == LeaveStatus.APPROVED;
    }

    private void reconcileEventReservation(LeaveApplication application, LeaveStatus oldStatus,
                                           LeaveDuration oldDuration, LeaveStatus newStatus,
                                           LeaveDuration newDuration) {
        if (eventLeaveEntitlementService == null || application.getEventEntitlementId() == null) {
            return;
        }
        BigDecimal oldAmount = countsAgainstEntitlement(oldStatus) ? applicationAmount(oldDuration) : BigDecimal.ZERO;
        BigDecimal newAmount = countsAgainstEntitlement(newStatus) ? applicationAmount(newDuration) : BigDecimal.ZERO;
        BigDecimal delta = newAmount.subtract(oldAmount);
        if (delta.signum() != 0) {
            eventLeaveEntitlementService.adjustReservation(application.getEventEntitlementId(), delta,
                    application.getLeaveDate(), application.getLeaveDate());
        }
    }

    private Optional<LeaveCalendar> calendarForStaff(Staff staff, LocalDate date) {
        if (staff.getTenantId() != null && staff.getJurisdictionId() != null) {
            return leaveCalendarService.getCalendarForTenantAndJurisdiction(
                    staff.getTenantId(), staff.getJurisdictionId(), date);
        }
        if (staff.getJurisdictionId() != null) {
            return leaveCalendarService.getCalendarForJurisdiction(staff.getJurisdictionId(), date);
        }
        return leaveCalendarService.getCalendarFor(date);
    }

    private boolean isPublicHoliday(LocalDate date, LeaveCalendar calendar) {
        return calendar.getPublicHolidays() != null && calendar.getPublicHolidays().stream()
                .map(PublicHoliday::getHolidayDate)
                .anyMatch(date::equals);
    }

    private List<LocalDate> getWorkingDatesInRange(Set<DayOfWeek> workingDays, LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            if (workingDays.contains(current.getDayOfWeek())) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private String resolveTenantId(LeaveApplication application) {
        if (application.getTenantId() != null && !application.getTenantId().isBlank()) {
            return application.getTenantId();
        }
        return application.getStaff() == null ? null : application.getStaff().getTenantId();
    }
}
