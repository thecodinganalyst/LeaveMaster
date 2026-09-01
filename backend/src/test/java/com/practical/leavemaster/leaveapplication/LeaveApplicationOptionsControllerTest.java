package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.leavetype.LeaveTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationOptionsControllerTest {

    @Mock
    private LeaveTypeService leaveTypeService;

    @InjectMocks
    private LeaveApplicationOptionsController controller;

    @Test
    void shouldReturnOnlyActiveLeaveTypesAsSelfServiceOptions() {
        LeaveType annual = LeaveType.builder()
                .id("annual")
                .name("Annual Leave")
                .active(true)
                .build();
        LeaveType disabled = LeaveType.builder()
                .id("disabled")
                .name("Disabled Leave")
                .active(false)
                .build();
        when(leaveTypeService.findAll()).thenReturn(List.of(annual, disabled));

        List<LeaveApplicationOptionsController.LeaveTypeOption> result = controller.getLeaveTypes();

        assertThat(result)
                .containsExactly(new LeaveApplicationOptionsController.LeaveTypeOption("annual", "Annual Leave"));
    }
}
