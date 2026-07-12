package com.practicallimits.spring_template.staff;

import com.practicallimits.spring_template.leavecalendar.LeaveCalendar;
import com.practicallimits.spring_template.leavecalendar.LeaveCalendarService;
import com.practicallimits.spring_template.leaveentitlement.LeaveEntitlement;
import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.leavetype.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @InjectMocks
    private StaffService staffService;

    @Test
    void shouldReturnAllStaff() {
        List<Staff> staffList = List.of(
                Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build(),
                Staff.builder().id("S002").name("Bob Jones").joinDate(LocalDate.of(2023, 6, 1)).workSchedule("WEEKDAYS").build()
        );
        when(staffRepository.findAll()).thenReturn(staffList);

        List<Staff> result = staffService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnStaffById() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        Optional<Staff> result = staffService.findById("S001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void shouldSaveStaff() {
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
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
                .workSchedule("WEEKDAYS")
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
    void shouldKeepManualEntitlementPeriodWithoutProration() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        Staff staff = Staff.builder()
                .id("S001")
                .name("Alice Smith")
                .joinDate(LocalDate.of(2023, 7, 1))
                .workSchedule("WEEKDAYS")
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
        Staff existing = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        Staff updated = Staff.builder().id("S001").name("Alice Johnson").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS_AND_SATURDAY").build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(existing));
        when(staffRepository.save(existing)).thenReturn(existing);

        Staff result = staffService.update("S001", updated);

        assertThat(result.getName()).isEqualTo("Alice Johnson");
        assertThat(result.getWorkSchedule()).isEqualTo("WEEKDAYS_AND_SATURDAY");
    }

    @Test
    void shouldUpdateStaffWithLeaveEntitlements() {
        LeaveType annual = LeaveType.builder().id("annual").name("Annual").used(true).build();
        LeaveCalendar leaveCalendar = LeaveCalendar.builder()
                .id("2023")
                .start(LocalDate.of(2023, 1, 1))
                .end(LocalDate.of(2023, 12, 31))
                .build();
        Staff existing = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 6, 1)).workSchedule("WEEKDAYS").build();
        Staff updated = Staff.builder()
                .id("S001")
                .name("Alice Johnson")
                .joinDate(LocalDate.of(2023, 6, 1))
                .workSchedule("WEEKDAYS_AND_SATURDAY")
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
        Staff staff = Staff.builder().id("S001").name("Alice Smith").joinDate(LocalDate.of(2023, 1, 1)).workSchedule("WEEKDAYS").build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));

        staffService.delete("S001");

        verify(staffRepository).deleteById("S001");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentStaff() {
        when(staffRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.delete("nonexistent"))
                .isInstanceOf(StaffNotFoundException.class);

        verify(staffRepository, never()).deleteById("nonexistent");
    }
}
