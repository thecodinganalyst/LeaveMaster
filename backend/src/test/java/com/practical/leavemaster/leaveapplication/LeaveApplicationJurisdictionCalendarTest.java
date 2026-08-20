package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.leaveapprover.LeaveApproverRepository;
import com.practical.leavemaster.leavecalendar.LeaveCalendar;
import com.practical.leavemaster.leavecalendar.LeaveCalendarService;
import com.practical.leavemaster.leavecalendar.PublicHoliday;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeRepository;
import com.practical.leavemaster.staff.DaySchedule;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.staff.WorkScheduleDay;
import com.practical.leavemaster.storage.StorageService;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationJurisdictionCalendarTest {

    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveCalendarService leaveCalendarService;
    @Mock private LeaveApproverRepository leaveApproverRepository;
    @Mock private EmailService emailService;
    @Mock private TenantActivityService tenantActivityService;
    @Mock private StorageService storageService;
    @InjectMocks private LeaveApplicationService leaveApplicationService;

    @Test
    void findByStaffAndDateUsesStaffJurisdictionCalendar() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        Staff staff = staff("SG");
        LeaveCalendar calendar = calendar("SG", List.of());
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveCalendarService.getCalendarFor("SG", date)).thenReturn(Optional.of(calendar));
        when(leaveApplicationRepository.findByStaffAndLeaveDateBetween(
                staff, calendar.getStart(), calendar.getEnd())).thenReturn(List.of());

        assertThat(leaveApplicationService.findByStaffId("S001", date)).isEmpty();
        verify(leaveCalendarService).getCalendarFor("SG", date);
        verify(leaveCalendarService, never()).getCalendarFor(date);
    }

    @Test
    void applySkipsPublicHolidayFromStaffJurisdictionCalendar() {
        LocalDate holidayDate = LocalDate.of(2026, 8, 20);
        Staff staff = staff("SG");
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(true).build();
        LeaveCalendar calendar = calendar("SG", List.of(PublicHoliday.builder()
                .holidayDate(holidayDate).holidayName("Holiday").build()));
        LeaveApplicationRequest request = LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(holidayDate)
                .toDate(holidayDate)
                .leaveTypeId("annual")
                .leaveDuration(LeaveDuration.FULL)
                .build();
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff));
        when(leaveTypeRepository.findById("annual")).thenReturn(Optional.of(leaveType));
        when(leaveCalendarService.getCalendarFor("SG", holidayDate)).thenReturn(Optional.of(calendar));

        assertThat(leaveApplicationService.apply(request, null)).isEmpty();
        verify(leaveApplicationRepository, never()).save(any());
    }

    private Staff staff(String jurisdictionId) {
        return Staff.builder()
                .id("S001")
                .name("Alice")
                .joinDate(LocalDate.of(2026, 1, 1))
                .tenantId("tenant-1")
                .jurisdictionId(jurisdictionId)
                .workSchedule(List.of(WorkScheduleDay.builder()
                        .dayOfWeek(DayOfWeek.THURSDAY)
                        .daySchedule(DaySchedule.FULL)
                        .build()))
                .build();
    }

    private LeaveCalendar calendar(String jurisdictionId, List<PublicHoliday> holidays) {
        return LeaveCalendar.builder()
                .id("calendar")
                .tenantId("tenant-1")
                .jurisdictionId(jurisdictionId)
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 12, 31))
                .publicHolidays(holidays)
                .build();
    }
}
