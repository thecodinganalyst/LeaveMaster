package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEmploymentDateValidatorTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private LeaveEmploymentDateValidator validator;

    private Staff staff(LocalDate joinDate, LocalDate termDate) {
        return Staff.builder()
                .id("S001")
                .name("Alice")
                .joinDate(joinDate)
                .termDate(termDate)
                .build();
    }

    private LeaveApplicationRequest request(LocalDate fromDate, LocalDate toDate) {
        return LeaveApplicationRequest.builder()
                .staffId("S001")
                .fromDate(fromDate)
                .toDate(toDate)
                .leaveTypeId("annual")
                .build();
    }

    @Test
    void shouldRejectLeaveBeforeJoinDate() {
        when(staffRepository.findById("S001"))
                .thenReturn(Optional.of(staff(LocalDate.of(2026, 9, 1), null)));

        assertThatThrownBy(() -> validator.validate(request(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot apply for leave before join date 2026-09-01");
    }

    @Test
    void shouldRejectLeaveAfterTerminationDate() {
        when(staffRepository.findById("S001"))
                .thenReturn(Optional.of(staff(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 31))));

        assertThatThrownBy(() -> validator.validate(request(
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot apply for leave after termination date 2026-08-31");
    }

    @Test
    void shouldAllowInclusiveJoinAndTerminationDates() {
        when(staffRepository.findById("S001"))
                .thenReturn(Optional.of(staff(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))));

        assertThatCode(() -> validator.validate(request(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowLeaveWithoutTerminationDateAfterJoinDate() {
        when(staffRepository.findById("S001"))
                .thenReturn(Optional.of(staff(LocalDate.of(2026, 9, 1), null)));

        assertThatCode(() -> validator.validate(request(
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11))))
                .doesNotThrowAnyException();
    }
}
