package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.leavecalendar.LeaveCalendar;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendarService;
import com.practicallimits.spring_template.leavecalendar.PublicHoliday;
import com.practicallimits.spring_template.leaveentitlement.LeaveEntitlement;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeNotFoundException;
import com.practicallimits.spring_template.leavetype.LeaveTypeRepository;
import com.practicallimits.spring_template.staff.DaySchedule;
import com.practicallimits.spring_template.staff.Staff;
import com.practicallimits.spring_template.staff.StaffNotFoundException;
import com.practicallimits.spring_template.staff.StaffRepository;
import com.practicallimits.spring_template.staff.WorkScheduleDay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationServiceTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveCalendarService leaveCalendarService;

    @InjectMocks
    private LeaveApplicationService leaveApplicationService;

    private Staff weekdayStaff() {
        return Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .workSchedule(List.of(
                        WorkScheduleDay.builder().dayOfWeek(DayOfWeek.MONDAY).daySchedule(DaySchedule.FULL).build(),
                        WorkScheduleDay.builder().dayOfWeek(DayOfWeek.TUESDAY).daySchedule(DaySchedule.FULL).build(),
                        WorkScheduleDay.builder().dayOfWeek(DayOfWeek.WEDNESDAY).daySchedule(DaySchedule.FULL).build(),
                        WorkScheduleDay.builder().dayOfWeek(DayOfWeek.THURSDAY).daySchedule(DaySchedule.FULL).build(),
                        WorkScheduleDay.builder().dayOfWeek(DayOfWeek.FRIDAY).daySchedule(DaySchedule.FULL).build()
                ))
                .build();
    }

    private LeaveType annualLeave() {
        return LeaveType.builder().id("annual").name("Annual Leave").used(true).build();
    }

    @Test
    void shouldApplyLeaveForWeekdaysInRange() {
        // 2024-01-08 (Mon) to 2024-01-12 (Fri) = 5 working days
        Staff staff = weekdayStaff();
        LeaveType leaveType = annualLeave();
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 12))
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.DRAFT)
                .build();

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType));
        when(leaveCalendarService.getCalendarFor(any(LocalDate.class))).thenReturn(Optional.empty());
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeaveApplication> result = leaveApplicationService.apply(request);

        assertThat(result).hasSize(5);
        assertThat(result).allMatch(a -> a.getStatus() == LeaveStatus.DRAFT);
        assertThat(result).allMatch(a -> a.getLeaveDuration() == LeaveDuration.FULL);
        assertThat(result).allMatch(a -> a.getLeaveType().getId().equals("annual"));
    }

    @Test
    void shouldExcludeWeekendsFromLeaveRange() {
        // 2024-01-08 (Mon) to 2024-01-14 (Sun) = 5 weekdays, weekend excluded
        Staff staff = weekdayStaff();
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 14))
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .build();

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annualLeave()));
        when(leaveCalendarService.getCalendarFor(any(LocalDate.class))).thenReturn(Optional.empty());
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeaveApplication> result = leaveApplicationService.apply(request);

        assertThat(result).hasSize(5);
        assertThat(result).noneMatch(a -> a.getLeaveDate().getDayOfWeek() == DayOfWeek.SATURDAY);
        assertThat(result).noneMatch(a -> a.getLeaveDate().getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    @Test
    void shouldExcludePublicHolidaysFromLeaveRange() {
        // 2024-01-08 (Mon) to 2024-01-12 (Fri), with 2024-01-10 (Wed) as public holiday
        Staff staff = weekdayStaff();
        LocalDate holiday = LocalDate.of(2024, 1, 10);
        LeaveCalendar calendar = LeaveCalendar.builder()
                .id("2024")
                .start(LocalDate.of(2024, 1, 1))
                .end(LocalDate.of(2024, 12, 31))
                .publicHolidays(List.of(PublicHoliday.builder().holidayDate(holiday).holidayName("New Year Observed").build()))
                .build();
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 12))
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .build();

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annualLeave()));
        when(leaveCalendarService.getCalendarFor(any(LocalDate.class))).thenReturn(Optional.of(calendar));
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeaveApplication> result = leaveApplicationService.apply(request);

        assertThat(result).hasSize(4);
        assertThat(result).noneMatch(a -> a.getLeaveDate().equals(holiday));
    }

    @Test
    void shouldDefaultStatusToDraftWhenNotSpecified() {
        Staff staff = weekdayStaff();
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 8))
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .build();

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annualLeave()));
        when(leaveCalendarService.getCalendarFor(any(LocalDate.class))).thenReturn(Optional.empty());
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeaveApplication> result = leaveApplicationService.apply(request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(LeaveStatus.DRAFT);
    }

    @Test
    void shouldSubmitForApprovalWithPendingStatus() {
        Staff staff = weekdayStaff();
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 8))
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.PENDING)
                .build();

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annualLeave()));
        when(leaveCalendarService.getCalendarFor(any(LocalDate.class))).thenReturn(Optional.empty());
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<LeaveApplication> result = leaveApplicationService.apply(request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void shouldThrowWhenStaffNotFound() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("nonexistent")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 8))
                .leaveTypeId("annual")
                .build();

        assertThatThrownBy(() -> leaveApplicationService.apply(request))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldThrowWhenLeaveTypeNotFound() {
        when(staffRepository.findById("S001")).thenReturn(Optional.of(weekdayStaff()));
        when(leaveTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 8))
                .toDate(LocalDate.of(2024, 1, 8))
                .leaveTypeId("nonexistent")
                .build();

        assertThatThrownBy(() -> leaveApplicationService.apply(request))
                .isInstanceOf(LeaveTypeNotFoundException.class);
    }

    @Test
    void shouldThrowWhenFromDateAfterToDate() {
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(LocalDate.of(2024, 1, 12))
                .toDate(LocalDate.of(2024, 1, 8))
                .leaveTypeId("annual")
                .build();

        assertThatThrownBy(() -> leaveApplicationService.apply(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromDate must be on or before toDate");
    }

    @Test
    void shouldReturnAllLeaveApplications() {
        Staff staff = weekdayStaff();
        List<LeaveApplication> applications = List.of(
                LeaveApplication.builder().id("id1").staff(staff).leaveDate(LocalDate.of(2024, 1, 8))
                        .leaveType(annualLeave()).leaveDuration(LeaveDuration.FULL).status(LeaveStatus.DRAFT)
                        .applicationDate(LocalDate.now()).build()
        );
        when(leaveApplicationRepository.findAll()).thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnLeaveApplicationsByStaffIdAndYear() {
        Staff staff = weekdayStaff();
        List<LeaveApplication> applications = List.of(
                LeaveApplication.builder().id("id1").staff(staff).leaveDate(LocalDate.of(2024, 3, 1))
                        .leaveType(annualLeave()).leaveDuration(LeaveDuration.FULL).status(LeaveStatus.APPROVED)
                        .applicationDate(LocalDate.of(2024, 2, 28)).build(),
                LeaveApplication.builder().id("id2").staff(staff).leaveDate(LocalDate.of(2024, 6, 10))
                        .leaveType(annualLeave()).leaveDuration(LeaveDuration.FULL).status(LeaveStatus.PENDING)
                        .applicationDate(LocalDate.of(2024, 6, 1)).build()
        );
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.findByStaffAndLeaveDateBetween(
                staff, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(applications);

        List<LeaveApplication> result = leaveApplicationService.findByStaffId("S001", 2024);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getLeaveDate().getYear() == 2024);
    }

    @Test
    void shouldReturnEmptyListWhenNoApplicationsForYear() {
        Staff staff = weekdayStaff();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.findByStaffAndLeaveDateBetween(
                staff, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .thenReturn(List.of());

        List<LeaveApplication> result = leaveApplicationService.findByStaffId("S001", 2023);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowWhenFindingByYearForNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveApplicationService.findByStaffId("nonexistent", 2024))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldThrowWhenInvalidYearProvided() {
        assertThatThrownBy(() -> leaveApplicationService.findByStaffId("S001", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid year: 0");
    }

    @Test
    void shouldReturnLeaveApplicationById() {
        LeaveApplication app = LeaveApplication.builder().id("id1").staff(weekdayStaff())
                .leaveDate(LocalDate.of(2024, 1, 8)).leaveType(annualLeave())
                .leaveDuration(LeaveDuration.FULL).status(LeaveStatus.DRAFT)
                .applicationDate(LocalDate.now()).build();
        when(leaveApplicationRepository.findById("id1")).thenReturn(Optional.of(app));

        Optional<LeaveApplication> result = leaveApplicationService.findById("id1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("id1");
    }

    @Test
    void shouldUpdateLeaveApplicationStatus() {
        LeaveApplication existing = LeaveApplication.builder().id("id1").staff(weekdayStaff())
                .leaveDate(LocalDate.of(2024, 1, 8)).leaveType(annualLeave())
                .leaveDuration(LeaveDuration.FULL).status(LeaveStatus.PENDING)
                .applicationDate(LocalDate.now()).build();
        LeaveApplication update = LeaveApplication.builder()
                .status(LeaveStatus.APPROVED)
                .approver(weekdayStaff())
                .approvalDate(LocalDate.now())
                .leaveDuration(LeaveDuration.FULL)
                .build();

        when(leaveApplicationRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplication result = leaveApplicationService.update("id1", update);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(result.getApprovalDate()).isNotNull();
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentLeaveApplication() {
        when(leaveApplicationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveApplicationService.update("nonexistent", new LeaveApplication()))
                .isInstanceOf(LeaveApplicationNotFoundException.class);
    }

    @Test
    void shouldDeleteLeaveApplication() {
        LeaveApplication app = LeaveApplication.builder().id("id1").staff(weekdayStaff())
                .leaveDate(LocalDate.of(2024, 1, 8)).leaveType(annualLeave())
                .leaveDuration(LeaveDuration.FULL).status(LeaveStatus.DRAFT)
                .applicationDate(LocalDate.now()).build();
        when(leaveApplicationRepository.findById("id1")).thenReturn(Optional.of(app));

        leaveApplicationService.delete("id1");

        verify(leaveApplicationRepository).deleteById("id1");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentLeaveApplication() {
        when(leaveApplicationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveApplicationService.delete("nonexistent"))
                .isInstanceOf(LeaveApplicationNotFoundException.class);

        verify(leaveApplicationRepository, never()).deleteById("nonexistent");
    }

    @Test
    void shouldReturnEmptyBalancesWhenStaffHasNoEntitlements() {
        Staff staff = weekdayStaff();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        List<LeaveBalance> balances = leaveApplicationService.getLeaveBalances("S001");

        assertThat(balances).isEmpty();
    }

    @Test
    void shouldReturnFullBalanceWhenNoLeaveUsed() {
        LeaveType leaveType = annualLeave();
        LeaveEntitlement entitlement = LeaveEntitlement.builder()
                .leaveType(leaveType)
                .from(LocalDate.of(2024, 1, 1))
                .to(LocalDate.of(2024, 12, 31))
                .entitlement(new BigDecimal("14.00"))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(entitlement))
                .build();
        entitlement.setStaff(staff);

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                eq(staff), eq(leaveType), any(), any(), any())).thenReturn(List.of());

        List<LeaveBalance> balances = leaveApplicationService.getLeaveBalances("S001");

        assertThat(balances).hasSize(1);
        assertThat(balances.getFirst().leaveType()).isEqualTo(leaveType);
        assertThat(balances.getFirst().entitlement()).isEqualByComparingTo(new BigDecimal("14.00"));
        assertThat(balances.getFirst().used()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balances.getFirst().balance()).isEqualByComparingTo(new BigDecimal("14.00"));
    }

    @Test
    void shouldDeductApprovedAndPendingLeaveFromBalance() {
        LeaveType leaveType = annualLeave();
        LeaveEntitlement entitlement = LeaveEntitlement.builder()
                .leaveType(leaveType)
                .from(LocalDate.of(2024, 1, 1))
                .to(LocalDate.of(2024, 12, 31))
                .entitlement(new BigDecimal("14.00"))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(entitlement))
                .build();
        entitlement.setStaff(staff);

        List<LeaveApplication> usedApplications = List.of(
                LeaveApplication.builder().leaveDate(LocalDate.of(2024, 3, 1))
                        .leaveType(leaveType).leaveDuration(LeaveDuration.FULL).status(LeaveStatus.APPROVED).build(),
                LeaveApplication.builder().leaveDate(LocalDate.of(2024, 3, 4))
                        .leaveType(leaveType).leaveDuration(LeaveDuration.FULL).status(LeaveStatus.PENDING).build()
        );

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                eq(staff), eq(leaveType), any(), any(), any())).thenReturn(usedApplications);

        List<LeaveBalance> balances = leaveApplicationService.getLeaveBalances("S001");

        assertThat(balances).hasSize(1);
        assertThat(balances.getFirst().used()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(balances.getFirst().balance()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    void shouldCountHalfDayLeaveAsHalfDay() {
        LeaveType leaveType = annualLeave();
        LeaveEntitlement entitlement = LeaveEntitlement.builder()
                .leaveType(leaveType)
                .from(LocalDate.of(2024, 1, 1))
                .to(LocalDate.of(2024, 12, 31))
                .entitlement(new BigDecimal("10.00"))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(entitlement))
                .build();
        entitlement.setStaff(staff);

        List<LeaveApplication> usedApplications = List.of(
                LeaveApplication.builder().leaveDate(LocalDate.of(2024, 3, 1))
                        .leaveType(leaveType).leaveDuration(LeaveDuration.AM).status(LeaveStatus.APPROVED).build()
        );

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                eq(staff), eq(leaveType), any(), any(), any())).thenReturn(usedApplications);

        List<LeaveBalance> balances = leaveApplicationService.getLeaveBalances("S001");

        assertThat(balances.getFirst().used()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(balances.getFirst().balance()).isEqualByComparingTo(new BigDecimal("9.5"));
    }

    @Test
    void shouldThrowWhenGettingBalancesForNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveApplicationService.getLeaveBalances("nonexistent"))
                .isInstanceOf(StaffNotFoundException.class);
    }
}
