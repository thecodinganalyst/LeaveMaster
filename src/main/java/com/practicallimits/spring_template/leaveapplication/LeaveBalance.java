package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.leavetype.LeaveType;

import java.math.BigDecimal;

public record LeaveBalance(
        LeaveType leaveType,
        BigDecimal entitlement,
        BigDecimal used,
        BigDecimal balance
) {}
