package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leavetype.LeaveType;

import java.math.BigDecimal;

public record LeaveBalance(
        LeaveType leaveType,
        BigDecimal entitlement,
        BigDecimal used,
        BigDecimal balance
) {}
