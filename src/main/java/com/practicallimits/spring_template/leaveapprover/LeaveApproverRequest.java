package com.practicallimits.spring_template.leaveapprover;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApproverRequest {

    private String staffId;
    private String approverId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String adminId;
}
