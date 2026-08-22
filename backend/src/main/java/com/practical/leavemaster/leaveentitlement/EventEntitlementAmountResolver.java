package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEvent;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.EventEntitlementAmountMode;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/** Resolves event grants into the day-equivalent units consumed by leave applications. */
@Component
public class EventEntitlementAmountResolver {

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    public BigDecimal resolve(Staff staff, LeaveEntitlementPolicy policy, QualifyingLeaveEvent event) {
        EventEntitlementAmountMode mode = policy.getEventEntitlementAmountMode() == null
                ? EventEntitlementAmountMode.FIXED
                : policy.getEventEntitlementAmountMode();
        return switch (mode) {
            case FIXED -> toWorkingDays(staff, policy.getEntitlementAmount(), policy.getEntitlementUnit());
            case APPROVED_EVENT_AMOUNT -> {
                BigDecimal approvedAmount = event.getApprovedEntitlementAmount();
                if (approvedAmount == null || approvedAmount.signum() <= 0) {
                    throw new IllegalArgumentException("A positive approved event entitlement amount is required");
                }
                if (policy.getEntitlementAmount() != null
                        && approvedAmount.compareTo(policy.getEntitlementAmount()) > 0) {
                    throw new IllegalArgumentException("Approved event entitlement amount exceeds the policy maximum");
                }
                yield toWorkingDays(staff, approvedAmount, policy.getEntitlementUnit());
            }
            case EVENT_PERIOD_WORKING_DAYS -> workingDaysInPeriod(staff, event);
        };
    }

    private BigDecimal toWorkingDays(Staff staff, BigDecimal amount, EntitlementUnit unit) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("A positive event entitlement amount is required");
        }
        if (unit == EntitlementUnit.DAYS) {
            return amount;
        }
        if (unit == EntitlementUnit.WEEKS) {
            return amount.multiply(weeklyWorkingDays(staff));
        }
        throw new IllegalArgumentException("Event entitlements in HOURS are not supported by day-based leave applications");
    }

    private BigDecimal workingDaysInPeriod(Staff staff, QualifyingLeaveEvent event) {
        if (event.getStartDate() == null || event.getEndDate() == null) {
            throw new IllegalArgumentException("Event startDate and endDate are required for event-period entitlements");
        }
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("Qualifying event entitlement period is invalid");
        }
        Map<java.time.DayOfWeek, DaySchedule> schedule = schedule(staff);
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = event.getStartDate(); !date.isAfter(event.getEndDate()); date = date.plusDays(1)) {
            total = total.add(dayAmount(schedule.get(date.getDayOfWeek())));
        }
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("Qualifying event period contains no scheduled working days");
        }
        return total;
    }

    private BigDecimal weeklyWorkingDays(Staff staff) {
        BigDecimal total = schedule(staff).values().stream()
                .map(this::dayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("A working schedule is required for week-based leave entitlement");
        }
        return total;
    }

    private Map<java.time.DayOfWeek, DaySchedule> schedule(Staff staff) {
        if (staff.getWorkSchedule() == null) {
            return Map.of();
        }
        return staff.getWorkSchedule().stream().collect(Collectors.toMap(
                WorkScheduleDay::getDayOfWeek,
                WorkScheduleDay::getDaySchedule,
                (first, ignored) -> first));
    }

    private BigDecimal dayAmount(DaySchedule schedule) {
        if (schedule == null) return BigDecimal.ZERO;
        return schedule == DaySchedule.FULL ? BigDecimal.ONE : HALF_DAY;
    }
}
