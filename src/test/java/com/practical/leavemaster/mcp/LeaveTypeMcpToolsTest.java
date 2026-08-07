package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveTypeMcpToolsTest {

    @Mock
    private LeaveTypeService leaveTypeService;

    @InjectMocks
    private LeaveTypeMcpTools leaveTypeMcpTools;

    @Test
    void shouldGetAllLeaveTypes() {
        List<LeaveType> leaveTypes = List.of(LeaveType.builder().id("annual").name("Annual Leave").used(false).build());
        when(leaveTypeService.findAll()).thenReturn(leaveTypes);

        List<LeaveType> result = leaveTypeMcpTools.getAllLeaveTypes();

        assertThat(result).hasSize(1);
        verify(leaveTypeService).findAll();
    }

    @Test
    void shouldGetLeaveTypeById() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeService.findById("annual")).thenReturn(Optional.of(leaveType));

        Optional<LeaveType> result = leaveTypeMcpTools.getLeaveTypeById("annual");

        assertThat(result).isPresent();
        verify(leaveTypeService).findById("annual");
    }

    @Test
    void shouldCreateLeaveType() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Annual Leave").used(false).build();
        when(leaveTypeService.save(leaveType)).thenReturn(leaveType);

        LeaveType result = leaveTypeMcpTools.createLeaveType(leaveType);

        assertThat(result.getId()).isEqualTo("annual");
        verify(leaveTypeService).save(leaveType);
    }

    @Test
    void shouldUpdateLeaveType() {
        LeaveType leaveType = LeaveType.builder().id("annual").name("Updated Leave").used(false).build();
        when(leaveTypeService.update("annual", leaveType)).thenReturn(leaveType);

        LeaveType result = leaveTypeMcpTools.updateLeaveType("annual", leaveType);

        assertThat(result.getName()).isEqualTo("Updated Leave");
        verify(leaveTypeService).update("annual", leaveType);
    }

    @Test
    void shouldDeleteLeaveType() {
        doNothing().when(leaveTypeService).delete("annual");

        leaveTypeMcpTools.deleteLeaveType("annual");

        verify(leaveTypeService).delete("annual");
    }
}
