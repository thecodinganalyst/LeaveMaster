package com.practicallimits.spring_template.leaveapplication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationRequest {

    private String staffId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String leaveTypeId;
    private LeaveDuration leaveDuration;
    private LeaveStatus status;
    private byte[] attachment;
}
