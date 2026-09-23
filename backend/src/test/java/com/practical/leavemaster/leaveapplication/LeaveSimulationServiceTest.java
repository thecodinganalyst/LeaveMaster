package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LeaveSimulationServiceTest {
    @Test
    void simulatesChargeableDaysAndBalanceWithoutSavingAnything() {
        StaffRepository staffRepository=mock(StaffRepository.class);
        LeaveApplicationRepository applicationRepository=mock(LeaveApplicationRepository.class);
        LeaveCalendarService calendarService=mock(LeaveCalendarService.class);
        LeaveType type=LeaveType.builder().id("AL").name("Annual Leave").build();
        Staff staff=staff(type);
        when(staffRepository.findById("EMP1")).thenReturn(Optional.of(staff));
        when(calendarService.getCalendarFor(eq("SG"), any(LocalDate.class))).thenReturn(Optional.of(LeaveCalendar.builder()
                .start(LocalDate.of(2026,1,1)).end(LocalDate.of(2026,12,31)).publicHolidays(new ArrayList<>()).build()));
        when(applicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(any(),any(),any(),any(),any()))
                .thenReturn(List.of(LeaveApplication.builder().leaveDuration(LeaveDuration.FULL).build()));

        var result=new LeaveSimulationService(staffRepository,applicationRepository,calendarService)
                .simulateLeaveUsage("EMP1","AL",LocalDate.of(2026,12,7),LocalDate.of(2026,12,11),LeaveDuration.FULL);

        assertThat(result.simulation()).isTrue();
        assertThat(result.chargeableDates()).hasSize(5);
        assertThat(result.simulatedCharge()).isEqualByComparingTo("5");
        assertThat(result.currentBalance()).isEqualByComparingTo("13");
        assertThat(result.projectedBalance()).isEqualByComparingTo("8");
        verify(staffRepository,never()).save(any());
        verify(applicationRepository,never()).save(any());
    }

    @Test
    void terminationProjectionUsesInclusiveDayHalfUpRuleWithoutMutation() {
        LeaveType type=LeaveType.builder().id("AL").name("Annual Leave").build();
        Staff staff=staff(type);
        StaffRepository repo=mock(StaffRepository.class);
        when(repo.findById("EMP1")).thenReturn(Optional.of(staff));
        var result=new LeaveSimulationService(repo,mock(LeaveApplicationRepository.class),mock(LeaveCalendarService.class))
                .simulateTerminationEntitlement("EMP1","AL",LocalDate.of(2026,6,30));
        assertThat(result.simulation()).isTrue();
        assertThat(result.projectedEntitlement()).isEqualByComparingTo("6.94");
        assertThat(staff.getTermDate()).isNull();
        assertThat(staff.getLeaveEntitlements().getFirst().getEntitlement()).isEqualByComparingTo("14");
        verify(repo,never()).save(any());
    }

    @Test
    void proposedPolicyProjectionDoesNotPersistPolicyOrEntitlement() {
        LeaveType type=LeaveType.builder().id("AL").name("Annual Leave").build();
        Staff staff=staff(type);
        staff.setJoinDate(LocalDate.of(2026,7,1));
        StaffRepository repo=mock(StaffRepository.class);
        when(repo.findById("EMP1")).thenReturn(Optional.of(staff));
        var result=new LeaveSimulationService(repo,mock(LeaveApplicationRepository.class),mock(LeaveCalendarService.class))
                .simulatePolicyValue("EMP1","AL",new BigDecimal("20"));
        assertThat(result.inputAmount()).isEqualByComparingTo("20");
        assertThat(result.projectedEntitlement()).isEqualByComparingTo("20");
        assertThat(staff.getLeaveEntitlements().getFirst().getEntitlement()).isEqualByComparingTo("14");
        verify(repo,never()).save(any());
    }

    private Staff staff(LeaveType type) {
        LeaveEntitlement entitlement=LeaveEntitlement.builder().leaveType(type).from(LocalDate.of(2026,1,1))
                .to(LocalDate.of(2026,12,31)).entitlement(new BigDecimal("14")).build();
        List<WorkScheduleDay> schedule=List.of(
                day(DayOfWeek.MONDAY),day(DayOfWeek.TUESDAY),day(DayOfWeek.WEDNESDAY),day(DayOfWeek.THURSDAY),day(DayOfWeek.FRIDAY));
        return Staff.builder().id("EMP1").tenantId("DEMO").jurisdictionId("SG").joinDate(LocalDate.of(2026,1,1))
                .workSchedule(schedule).leaveEntitlements(new ArrayList<>(List.of(entitlement))).build();
    }
    private WorkScheduleDay day(DayOfWeek d){ return WorkScheduleDay.builder().dayOfWeek(d).daySchedule(DaySchedule.FULL).build(); }
}
