package com.practical.leavemaster.staff;

import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private LeaveCalendarService leaveCalendarService;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveApproverRepository leaveApproverRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private StaffService staffService;

    private static List<WorkScheduleDay> weekdays() {
        return List.of(
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.MONDAY).daySchedule(DaySchedule.FULL).build(),
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.TUESDAY).daySchedule(DaySchedule.FULL).build(),
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.WEDNESDAY).daySchedule(DaySchedule.FULL).build(),
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.THURSDAY).daySchedule(DaySchedule.FULL).build(),
                WorkScheduleDay.builder().dayOfWeek(DayOfWeek.FRIDAY).daySchedule(DaySchedule.FULL).build()
        );
    }

    private static List<WorkScheduleDay> weekdaysAndSaturday() {
        List<WorkScheduleDay> schedule = new ArrayList<>(weekdays());
        schedule.add(WorkScheduleDay.builder().dayOfWeek(DayOfWeek.SATURDAY).daySchedule(DaySchedule.FULL).build());
        return schedule;
    }

    @Test
    void shouldReturnAllStaff() {
        List<Staff> staffList = List.of(
                Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build(),
                Staff.builder().id("S002").name("Bob Jones").joinDate(LocalDate.of(2023, 6, 1)).workSchedule(weekdays()).build()
        );
        when(staffRepository.findAll()).thenReturn(staffList);

        List<Staff> result = staffService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnStaffById() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        Optional<Staff> result = staffService.findById("S001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void shouldSaveStaff() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build();
        when(staffRepository.save(staff)).thenReturn(staff);

        Staff result = staffService.save(staff);

        assertThat(result.getId()).isEqualTo("S001");
    }

    @Test
    void shouldSaveStaffAndProrateEntitlementUsingJoinDateCalendar() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("2023")
                .start(LocalDate.of(2023, 1, 1))
                .end(LocalDate.of(2023, 12, 31))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 7, 1))
                .workSchedule(weekdays())
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2023, 7, 1))).thenReturn(Optional.of(leaveCalendar));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff saved = staffService.save(staff);

        assertThat(saved.getLeaveEntitlements()).hasSize(1);
        LeaveEntitlement leaveEntitlement = saved.getLeaveEntitlements().getFirst();
        assertThat(leaveEntitlement.getFrom()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(leaveEntitlement.getTo()).isEqualTo(LocalDate.of(2023, 12, 31));
        // 20.00 * (184 remaining days / 365 total days) = 10.08
        assertThat(leaveEntitlement.getEntitlement()).isEqualByComparingTo("10.08");
        assertThat(leaveEntitlement.getStaff()).isEqualTo(saved);
        assertThat(leaveEntitlement.getLeaveType().getId()).isEqualTo("annual");
    }

    @Test
    void shouldMarkLeaveTypeAsUsedWhenSavingEntitlement() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(false).build();
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("2023")
                .start(LocalDate.of(2023, 1, 1))
                .end(LocalDate.of(2023, 12, 31))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 7, 1))
                .workSchedule(weekdays())
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2023, 7, 1))).thenReturn(Optional.of(leaveCalendar));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        staffService.save(staff);

        assertThat(annual.isUsed()).isTrue();
        verify(leaveTypeRepository).save(annual);
    }

    @Test
    void shouldKeepManualEntitlementPeriodWithoutProration() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 7, 1))
                .workSchedule(weekdays())
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .from(LocalDate.of(2023, 7, 1))
                        .to(LocalDate.of(2023, 12, 31))
                        .entitlement(new BigDecimal("12.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff saved = staffService.save(staff);

        assertThat(saved.getLeaveEntitlements().getFirst().getEntitlement()).isEqualByComparingTo("12.00");
        verifyNoInteractions(leaveCalendarService);
    }

    @Test
    void shouldUpdateStaff() {
        Staff existing = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build();
        Staff updated = Staff.builder().id("S001").name("Alice Johnson").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdaysAndSaturday()).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        assertThat(result.getName()).isEqualTo("Alice Johnson");
        assertThat(result.getWorkSchedule()).hasSize(6);
    }

    @Test
    void shouldUpdateStaffWithLeaveEntitlements() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("2023")
                .start(LocalDate.of(2023, 1, 1))
                .end(LocalDate.of(2023, 12, 31))
                .build();
        Staff existing = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 6, 1)).workSchedule(weekdays()).build();
        Staff updated = Staff.builder()
                .id("S001")
                .name("Alice Johnson")
                .joinDate(LocalDate.of(2023, 6, 1))
                .workSchedule(weekdaysAndSaturday())
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2023, 6, 1))).thenReturn(Optional.of(leaveCalendar));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        assertThat(result.getLeaveEntitlements()).hasSize(1);
        assertThat(result.getLeaveEntitlements().getFirst().getFrom()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(result.getLeaveEntitlements().getFirst().getTo()).isEqualTo(LocalDate.of(2023, 12, 31));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.update("nonexistent", new Staff()))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldDeleteStaff() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        staffService.delete("S001");

        verify(staffRepository).deleteById("S001");
    }

    @Test
    void shouldThrowWhenDeletingStaffWithLeaveApplications() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.existsByStaffId("S001")).thenReturn(true);

        assertThatThrownBy(() -> staffService.delete("S001"))
                .isInstanceOf(StaffInUseException.class);

        verify(staffRepository, never()).deleteById("S001");
    }

    @Test
    void shouldThrowWhenDeletingStaffWithLeaveApprovals() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule(weekdays()).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveApplicationRepository.existsByApproverId("S001")).thenReturn(true);

        assertThatThrownBy(() -> staffService.delete("S001"))
                .isInstanceOf(StaffInUseException.class);

        verify(staffRepository, never()).deleteById("S001");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.delete("nonexistent"))
                .isInstanceOf(StaffNotFoundException.class);

        verify(staffRepository, never()).deleteById("nonexistent");
    }

    @Test
    void shouldTerminateStaffAndProrateEntitlement() {
        LeaveEntitlement entitlement = LeaveEntitlement.builder()
                .leaveType(LeaveType.builder().id("annual").name("Annual").build())
                .from(LocalDate.of(2024, 1, 1))
                .to(LocalDate.of(2024, 12, 31))
                .entitlement(new BigDecimal("20.00"))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2024, 1, 1))
                .workSchedule(weekdays())
                .leaveEntitlements(new ArrayList<>(List.of(entitlement)))
                .build();
        entitlement.setStaff(staff);

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(staffRepository.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));
        when(leaveApproverRepository.findByApprover(staff)).thenReturn(List.of());

        LocalDate termDate = LocalDate.of(2024, 6, 30);
        TerminationResult result = staffService.terminate("S001", termDate);

        assertThat(result.getStaff().getTermDate()).isEqualTo(termDate);
        LeaveEntitlement updated = result.getStaff().getLeaveEntitlements().getFirst();
        assertThat(updated.getTo()).isEqualTo(termDate);
        // 20 * 182 days (Jan 1 to Jun 30) / 366 days (Jan 1 to Dec 31 in 2024) = 9.95
        assertThat(updated.getEntitlement()).isEqualByComparingTo("9.95");
        assertThat(result.getStaffWithNoApprover()).isEmpty();
    }

    @Test
    void shouldTerminateStaffAndProrateEntitlementWithJoinDateProration() {
        // Staff joined Jun 1 with already-prorated entitlement, then terminated Aug 31
        LeaveEntitlement entitlement = LeaveEntitlement.builder()
                .leaveType(LeaveType.builder().id("annual").name("Annual").build())
                .from(LocalDate.of(2024, 1, 1))
                .to(LocalDate.of(2024, 12, 31))
                .entitlement(new BigDecimal("11.75"))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2024, 6, 1))
                .workSchedule(weekdays())
                .leaveEntitlements(new ArrayList<>(List.of(entitlement)))
                .build();
        entitlement.setStaff(staff);

        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(staffRepository.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));
        when(leaveApproverRepository.findByApprover(staff)).thenReturn(List.of());

        LocalDate termDate = LocalDate.of(2024, 8, 31);
        TerminationResult result = staffService.terminate("S001", termDate);

        assertThat(result.getStaff().getTermDate()).isEqualTo(termDate);
        LeaveEntitlement updated = result.getStaff().getLeaveEntitlements().getFirst();
        assertThat(updated.getTo()).isEqualTo(termDate);
        // effectiveFrom = Jun 1 (joinDate > entitlement.from)
        // workedDays = Jun 1 to Aug 31 = 92 days; totalEffectiveDays = Jun 1 to Dec 31 = 214 days
        // terminationProrated = 11.75 * 92 / 214 = 5.05
        assertThat(updated.getEntitlement()).isEqualByComparingTo("5.05");
    }

    @Test
    void shouldTerminateApproverAndReturnStaffWithNoApprover() {
        Staff approver = Staff.builder().id("S002").name("Bob").joinDate(LocalDate.of(2023, 1, 1)).build();
        Staff staffMember = Staff.builder().id("S001").name("Alice").joinDate(LocalDate.of(2023, 1, 1)).build();

        LeaveApprover approverRecord = LeaveApprover.builder()
                .id("la1")
                .staff(staffMember)
                .approver(approver)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(approver)
                .adminDate(LocalDate.of(2023, 12, 1))
                .build();

        when(staffRepository.findById("S002")).thenReturn(Optional.of(approver));
        when(staffRepository.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));
        when(leaveApproverRepository.findByApprover(approver)).thenReturn(List.of(approverRecord));
        when(leaveApproverRepository.findActiveApproversForStaff(eq(staffMember), any(LocalDate.class)))
                .thenReturn(List.of());

        LocalDate termDate = LocalDate.of(2024, 6, 30);
        TerminationResult result = staffService.terminate("S002", termDate);

        assertThat(result.getStaff().getTermDate()).isEqualTo(termDate);
        assertThat(approverRecord.getEffectiveTo()).isEqualTo(termDate);
        verify(leaveApproverRepository).save(approverRecord);
        assertThat(result.getStaffWithNoApprover()).containsExactly(staffMember);
    }

    @Test
    void shouldNotListStaffWithNoApproverWhenOtherApproverExists() {
        Staff approver = Staff.builder().id("S002").name("Bob").joinDate(LocalDate.of(2023, 1, 1)).build();
        Staff staffMember = Staff.builder().id("S001").name("Alice").joinDate(LocalDate.of(2023, 1, 1)).build();
        Staff otherApprover = Staff.builder().id("S003").name("Carol").joinDate(LocalDate.of(2023, 1, 1)).build();

        LeaveApprover approverRecord = LeaveApprover.builder()
                .id("la1")
                .staff(staffMember)
                .approver(approver)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(approver)
                .adminDate(LocalDate.of(2023, 12, 1))
                .build();
        LeaveApprover remainingApproverRecord = LeaveApprover.builder()
                .id("la2")
                .staff(staffMember)
                .approver(otherApprover)
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .admin(approver)
                .adminDate(LocalDate.of(2023, 12, 1))
                .build();

        when(staffRepository.findById("S002")).thenReturn(Optional.of(approver));
        when(staffRepository.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));
        when(leaveApproverRepository.findByApprover(approver)).thenReturn(List.of(approverRecord));
        when(leaveApproverRepository.findActiveApproversForStaff(eq(staffMember), any(LocalDate.class)))
                .thenReturn(List.of(remainingApproverRecord));

        TerminationResult result = staffService.terminate("S002", LocalDate.of(2024, 6, 30));

        assertThat(result.getStaffWithNoApprover()).isEmpty();
    }

    @Test
    void shouldThrowWhenTerminationDateBeforeJoinDate() {
        Staff staff = Staff.builder().id("S001").name("Alice").joinDate(LocalDate.of(2024, 1, 1)).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> staffService.terminate("S001", LocalDate.of(2023, 12, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Termination date must not be before join date");
    }

    @Test
    void shouldThrowWhenTerminatingNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.terminate("nonexistent", LocalDate.of(2024, 6, 30)))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    void shouldThrowWhenTerminationDateIsNull() {
        Staff staff = Staff.builder().id("S001").name("Alice").joinDate(LocalDate.of(2024, 1, 1)).build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> staffService.terminate("S001", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Termination date is required");
    }

    @Test
    void shouldThrowWhenLeaveEntitlementHasNoLeaveType() {
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Leave entitlement must specify a leave type ID");
    }

    @Test
    void shouldThrowWhenLeaveTypeNotFoundDuringSave() {
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("nonexistent").build())
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Leave type not found");
    }

    @Test
    void shouldThrowWhenLeaveEntitlementFromIsAfterTo() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .from(LocalDate.of(2023, 12, 31))
                        .to(LocalDate.of(2023, 1, 1))
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be on or before to");
    }

    @Test
    void shouldThrowWhenOnlyOneOfFromOrToIsProvided() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .from(LocalDate.of(2023, 1, 1))
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires both from and to");
    }

    @Test
    void shouldThrowWhenNoLeaveCalendarFoundForJoinDate() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 1, 1))
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .entitlement(new BigDecimal("20.00"))
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2023, 1, 1))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No leave calendar found for join date");
    }

    @Test
    void shouldThrowWhenEntitlementAmountIsNull() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("2023")
                .start(LocalDate.of(2023, 1, 1))
                .end(LocalDate.of(2023, 12, 31))
                .build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 7, 1))
                .leaveEntitlements(List.of(LeaveEntitlement.builder()
                        .leaveType(LeaveType.builder().id("annual").build())
                        .entitlement(null)
                        .build()))
                .build();

        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(annual));
        when(leaveCalendarService.getCalendarFor(LocalDate.of(2023, 7, 1))).thenReturn(Optional.of(leaveCalendar));

        assertThatThrownBy(() -> staffService.save(staff))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Leave entitlement amount is required");
    }
}
