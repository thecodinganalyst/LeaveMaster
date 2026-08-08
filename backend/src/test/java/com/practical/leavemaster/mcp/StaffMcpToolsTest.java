package com.practical.leavemaster.mcp;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffService;
import com.practical.leavemaster.staff.TerminationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffMcpToolsTest {

    @Mock
    private StaffService staffService;

    @InjectMocks
    private StaffMcpTools staffMcpTools;

    @Test
    void shouldGetAllStaff() {
        List<Staff> staff = List.of(Staff.builder().id("s1").name("Alice").build());
        when(staffService.findAll()).thenReturn(staff);

        List<Staff> result = staffMcpTools.getAllStaff();

        assertThat(result).hasSize(1);
        verify(staffService).findAll();
    }

    @Test
    void shouldGetStaffById() {
        Staff staff = Staff.builder().id("s1").name("Alice").build();
        when(staffService.findById("s1")).thenReturn(Optional.of(staff));

        Optional<Staff> result = staffMcpTools.getStaffById("s1");

        assertThat(result).isPresent();
        verify(staffService).findById("s1");
    }

    @Test
    void shouldCreateStaff() {
        Staff staff = Staff.builder().id("s1").name("Alice").joinDate(LocalDate.now()).build();
        when(staffService.save(staff)).thenReturn(staff);

        Staff result = staffMcpTools.createStaff(staff);

        assertThat(result.getId()).isEqualTo("s1");
        verify(staffService).save(staff);
    }

    @Test
    void shouldUpdateStaff() {
        Staff staff = Staff.builder().id("s1").name("Alice Updated").joinDate(LocalDate.now()).build();
        when(staffService.update("s1", staff)).thenReturn(staff);

        Staff result = staffMcpTools.updateStaff("s1", staff);

        assertThat(result.getName()).isEqualTo("Alice Updated");
        verify(staffService).update("s1", staff);
    }

    @Test
    void shouldDeleteStaff() {
        doNothing().when(staffService).delete("s1");

        staffMcpTools.deleteStaff("s1");

        verify(staffService).delete("s1");
    }

    @Test
    void shouldTerminateStaff() {
        LocalDate termDate = LocalDate.now();
        Staff staff = Staff.builder().id("s1").name("Alice").joinDate(LocalDate.now()).build();
        TerminationResult terminationResult = new TerminationResult(staff, List.of());
        when(staffService.terminate("s1", termDate)).thenReturn(terminationResult);

        TerminationResult result = staffMcpTools.terminateStaff("s1", termDate);

        assertThat(result.getStaff().getId()).isEqualTo("s1");
        verify(staffService).terminate("s1", termDate);
    }
}
