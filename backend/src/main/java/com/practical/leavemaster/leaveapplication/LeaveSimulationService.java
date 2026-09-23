package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveSimulationService {
    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");
    private static final long INCLUSIVE_DAY_OFFSET = 1L;

    private final StaffRepository staffRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final LeaveCalendarService calendarService;

    @Transactional(readOnly = true)
    public LeaveUsageSimulation simulateLeaveUsage(String staffId, String leaveTypeId, LocalDate from, LocalDate to,
                                                    LeaveDuration duration) {
        requireRange(from, to);
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new StaffNotFoundException(staffId));
        validateEmploymentDates(staff, from, to);
        LeaveEntitlement entitlement = staff.getLeaveEntitlements().stream()
                .filter(e -> e.getLeaveType() != null && leaveTypeId.equals(e.getLeaveType().getId())
                        && !from.isBefore(e.getFrom()) && !to.isAfter(e.getTo()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No matching entitlement covers the simulated dates"));
        LeaveDuration resolvedDuration = duration == null ? LeaveDuration.FULL : duration;
        Set<DayOfWeek> workDays = staff.getWorkSchedule().stream().map(WorkScheduleDay::getDayOfWeek).collect(Collectors.toSet());
        List<LocalDate> chargeableDates = from.datesUntil(to.plusDays(1))
                .filter(date -> workDays.contains(date.getDayOfWeek()))
                .filter(date -> calendarForStaff(staff, date).map(c -> !isPublicHoliday(date, c)).orElse(true))
                .toList();
        BigDecimal simulatedCharge = amount(resolvedDuration).multiply(BigDecimal.valueOf(chargeableDates.size()));
        List<LeaveStatus> counted = List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING);
        BigDecimal currentUsed = applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                        staff, entitlement.getLeaveType(), entitlement.getFrom(), entitlement.getTo(), counted)
                .stream().map(a -> amount(a.getLeaveDuration())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentBalance = entitlement.getEntitlement().subtract(currentUsed);
        return new LeaveUsageSimulation(true, staffId, leaveTypeId, from, to, resolvedDuration,
                chargeableDates, simulatedCharge, entitlement.getEntitlement(), currentUsed, currentBalance,
                currentBalance.subtract(simulatedCharge), staff.getJurisdictionId(),
                List.of("Uses the staff work schedule", "Excludes configured public holidays",
                        "Current used amount counts APPROVED and PENDING leave", "No leave request is created"));
    }

    @Transactional(readOnly = true)
    public EntitlementSimulation simulateTerminationEntitlement(String staffId, String leaveTypeId, LocalDate terminationDate) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new StaffNotFoundException(staffId));
        if (terminationDate == null || terminationDate.isBefore(staff.getJoinDate())) {
            throw new IllegalArgumentException("Simulated termination date must not be before join date");
        }
        LeaveEntitlement entitlement = findEntitlement(staff, leaveTypeId);
        LocalDate effectiveFrom = staff.getJoinDate() != null && staff.getJoinDate().isAfter(entitlement.getFrom())
                ? staff.getJoinDate() : entitlement.getFrom();
        LocalDate effectiveTo = terminationDate.isBefore(entitlement.getTo()) ? terminationDate : entitlement.getTo();
        BigDecimal projected = prorate(entitlement.getEntitlement(), effectiveFrom, effectiveTo, entitlement.getTo());
        return new EntitlementSimulation(true, staffId, leaveTypeId, entitlement.getEntitlement(), projected,
                effectiveFrom, effectiveTo, staff.getJurisdictionId(),
                List.of("Uses the current stored entitlement as the authoritative starting amount",
                        "Uses the same inclusive-day HALF_UP proration rule as termination processing",
                        "Does not change the staff termination date or entitlement"));
    }

    @Transactional(readOnly = true)
    public EntitlementSimulation simulatePolicyValue(String staffId, String leaveTypeId, BigDecimal proposedFullPeriodAmount) {
        if (proposedFullPeriodAmount == null || proposedFullPeriodAmount.signum() < 0) {
            throw new IllegalArgumentException("proposedFullPeriodAmount must be zero or positive");
        }
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new StaffNotFoundException(staffId));
        LeaveEntitlement entitlement = findEntitlement(staff, leaveTypeId);
        LocalDate effectiveFrom = staff.getJoinDate() != null && staff.getJoinDate().isAfter(entitlement.getFrom())
                ? staff.getJoinDate() : entitlement.getFrom();
        LocalDate effectiveTo = staff.getTermDate() != null && staff.getTermDate().isBefore(entitlement.getTo())
                ? staff.getTermDate() : entitlement.getTo();
        BigDecimal projected = prorate(proposedFullPeriodAmount, effectiveFrom, effectiveTo, entitlement.getTo());
        return new EntitlementSimulation(true, staffId, leaveTypeId, proposedFullPeriodAmount, projected,
                effectiveFrom, effectiveTo, staff.getJurisdictionId(),
                List.of("Proposed policy value is hypothetical", "Uses current employment dates and entitlement period",
                        "Uses the same inclusive-day HALF_UP proration rule", "No policy or entitlement is persisted"));
    }

    private LeaveEntitlement findEntitlement(Staff staff, String leaveTypeId) {
        return staff.getLeaveEntitlements().stream()
                .filter(e -> e.getLeaveType() != null && leaveTypeId.equals(e.getLeaveType().getId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Leave entitlement not found"));
    }

    private BigDecimal prorate(BigDecimal fullAmount, LocalDate effectiveFrom, LocalDate effectiveTo, LocalDate periodTo) {
        if (effectiveTo.isBefore(effectiveFrom)) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long total = ChronoUnit.DAYS.between(effectiveFrom, periodTo) + INCLUSIVE_DAY_OFFSET;
        long effective = ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) + INCLUSIVE_DAY_OFFSET;
        if (effective >= total) return fullAmount;
        return fullAmount.multiply(BigDecimal.valueOf(effective)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("from and to are required");
        if (from.isAfter(to)) throw new IllegalArgumentException("from must be on or before to");
    }
    private void validateEmploymentDates(Staff staff, LocalDate from, LocalDate to) {
        if (staff.getJoinDate() != null && from.isBefore(staff.getJoinDate())) throw new IllegalArgumentException("Simulated leave cannot start before employment");
        if (staff.getTermDate() != null && to.isAfter(staff.getTermDate())) throw new IllegalArgumentException("Simulated leave cannot end after termination");
    }
    private BigDecimal amount(LeaveDuration duration) { return duration == LeaveDuration.FULL ? BigDecimal.ONE : HALF_DAY; }
    private Optional<LeaveCalendar> calendarForStaff(Staff staff, LocalDate date) {
        return staff.getJurisdictionId() == null || staff.getJurisdictionId().isBlank()
                ? calendarService.getCalendarFor(date) : calendarService.getCalendarFor(staff.getJurisdictionId(), date);
    }
    private boolean isPublicHoliday(LocalDate date, LeaveCalendar calendar) {
        return calendar.getPublicHolidays().stream().anyMatch(h -> date.equals(h.getHolidayDate()));
    }

    public record LeaveUsageSimulation(boolean simulation, String staffId, String leaveTypeId, LocalDate from, LocalDate to,
            LeaveDuration duration, List<LocalDate> chargeableDates, BigDecimal simulatedCharge,
            BigDecimal entitlement, BigDecimal currentUsed, BigDecimal currentBalance, BigDecimal projectedBalance,
            String jurisdictionId, List<String> assumptions) {}
    public record EntitlementSimulation(boolean simulation, String staffId, String leaveTypeId,
            BigDecimal inputAmount, BigDecimal projectedEntitlement, LocalDate effectiveFrom, LocalDate effectiveTo,
            String jurisdictionId, List<String> assumptions) {}
}
