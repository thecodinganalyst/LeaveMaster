package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leaveeligibility.QualifyingEventStatus;
import com.practical.leavemaster.leaveeligibility.QualifyingLeaveEvent;
import com.practical.leavemaster.leaveentitlementpolicy.EntitlementUnit;
import com.practical.leavemaster.leaveentitlementpolicy.EventEntitlementAmountMode;
import com.practical.leavemaster.leaveentitlementpolicy.LeaveEntitlementPolicy;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEntitlementAmountResolverTest {

    private final EventEntitlementAmountResolver resolver = new EventEntitlementAmountResolver();

    @Test
    void convertsWeeksUsingActualWeeklySchedule() {
        LeaveEntitlementPolicy policy = policy(EventEntitlementAmountMode.FIXED, EntitlementUnit.WEEKS, "4");
        assertThat(resolver.resolve(fiveDayStaff(), policy, event(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3))))
                .isEqualByComparingTo("20");
    }

    @Test
    void defaultsMissingModeToFixedAndReturnsDayAmountDirectly() {
        LeaveEntitlementPolicy policy = policy(null, EntitlementUnit.DAYS, "3");
        assertThat(resolver.resolve(fiveDayStaff(), policy, event(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3))))
                .isEqualByComparingTo("3");
    }

    @Test
    void approvedAllocationIsRequiredAndCappedByPolicyMaximum() {
        LeaveEntitlementPolicy policy = policy(EventEntitlementAmountMode.APPROVED_EVENT_AMOUNT, EntitlementUnit.WEEKS, "10");
        QualifyingLeaveEvent event = event(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3));

        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive approved event entitlement amount");

        event.setApprovedEntitlementAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive approved event entitlement amount");

        event.setApprovedEntitlementAmount(new BigDecimal("6"));
        assertThat(resolver.resolve(fiveDayStaff(), policy, event)).isEqualByComparingTo("30");

        event.setApprovedEntitlementAmount(new BigDecimal("11"));
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the policy maximum");
    }

    @Test
    void approvedAllocationWithoutMaximumUsesApprovedAmount() {
        LeaveEntitlementPolicy policy = LeaveEntitlementPolicy.builder()
                .eventEntitlementAmountMode(EventEntitlementAmountMode.APPROVED_EVENT_AMOUNT)
                .entitlementUnit(EntitlementUnit.DAYS)
                .build();
        QualifyingLeaveEvent event = event(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3));
        event.setApprovedEntitlementAmount(new BigDecimal("2.5"));
        assertThat(resolver.resolve(fiveDayStaff(), policy, event)).isEqualByComparingTo("2.5");
    }

    @Test
    void eventPeriodCountsFullAndHalfScheduledWorkingDays() {
        LeaveEntitlementPolicy policy = policy(EventEntitlementAmountMode.EVENT_PERIOD_WORKING_DAYS, EntitlementUnit.DAYS, "1");
        QualifyingLeaveEvent event = event(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 10));
        assertThat(resolver.resolve(fiveDayStaff(), policy, event)).isEqualByComparingTo("2");

        Staff halfDayStaff = Staff.builder().workSchedule(List.of(
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.FRIDAY).daySchedule(DaySchedule.AM).build(),
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.MONDAY).daySchedule(DaySchedule.PM).build()
        )).build();
        assertThat(resolver.resolve(halfDayStaff, policy, event)).isEqualByComparingTo("1");
    }

    @Test
    void rejectsInvalidFixedAmountsUnsupportedUnitsAndMissingSchedules() {
        QualifyingLeaveEvent event = event(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3));
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy(EventEntitlementAmountMode.FIXED, EntitlementUnit.DAYS, "0"), event))
                .hasMessageContaining("positive event entitlement amount");
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy(EventEntitlementAmountMode.FIXED, EntitlementUnit.HOURS, "8"), event))
                .hasMessageContaining("HOURS");
        assertThatThrownBy(() -> resolver.resolve(Staff.builder().build(), policy(EventEntitlementAmountMode.FIXED, EntitlementUnit.WEEKS, "1"), event))
                .hasMessageContaining("working schedule");
    }

    @Test
    void rejectsMissingReversedAndNonWorkingEventPeriods() {
        LeaveEntitlementPolicy policy = policy(EventEntitlementAmountMode.EVENT_PERIOD_WORKING_DAYS, EntitlementUnit.DAYS, "1");
        QualifyingLeaveEvent missing = QualifyingLeaveEvent.builder().eventDate(LocalDate.of(2026, 8, 3)).build();
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy, missing))
                .hasMessageContaining("startDate and endDate");

        QualifyingLeaveEvent reversed = event(LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 3));
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy, reversed))
                .hasMessageContaining("period is invalid");

        QualifyingLeaveEvent weekend = event(LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 9));
        assertThatThrownBy(() -> resolver.resolve(fiveDayStaff(), policy, weekend))
                .hasMessageContaining("no scheduled working days");
    }

    private LeaveEntitlementPolicy policy(EventEntitlementAmountMode mode, EntitlementUnit unit, String amount) {
        return LeaveEntitlementPolicy.builder()
                .eventEntitlementAmountMode(mode)
                .entitlementUnit(unit)
                .entitlementAmount(new BigDecimal(amount))
                .build();
    }

    private QualifyingLeaveEvent event(LocalDate start, LocalDate end) {
        return QualifyingLeaveEvent.builder()
                .eventDate(start)
                .startDate(start)
                .endDate(end)
                .status(QualifyingEventStatus.VERIFIED)
                .build();
    }

    private Staff fiveDayStaff() {
        return Staff.builder().id("staff-1").tenantId("tenant-1").workSchedule(List.of(
                day(DayOfWeek.MONDAY),
                day(DayOfWeek.TUESDAY),
                day(DayOfWeek.WEDNESDAY),
                day(DayOfWeek.THURSDAY),
                day(DayOfWeek.FRIDAY)
        )).build();
    }

    private WorkScheduleDay day(DayOfWeek day) {
        return WorkScheduleDay.builder().dayOfWeek(day).daySchedule(DaySchedule.FULL).build();
    }
}
