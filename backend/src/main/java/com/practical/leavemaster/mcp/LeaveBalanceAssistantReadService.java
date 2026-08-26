package com.practical.leavemaster.mcp;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveApplicationRepository;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffNotFoundException;
import com.practical.leavemaster.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveBalanceAssistantReadService {

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    private final StaffRepository staffRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    @Transactional(readOnly = true)
    public List<LeaveBalanceResult> findByStaffId(String staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        List<LeaveStatus> countedStatuses = List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING);
        List<LeaveBalanceResult> results = new ArrayList<>();

        for (LeaveEntitlement entitlement : staff.getLeaveEntitlements()) {
            LeaveType leaveType = entitlement.getLeaveType();
            List<LeaveApplication> applications = leaveApplicationRepository
                    .findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
                            staff, leaveType, entitlement.getFrom(), entitlement.getTo(), countedStatuses);
            BigDecimal used = applications.stream()
                    .map(this::applicationAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            results.add(new LeaveBalanceResult(
                    leaveType.getId(),
                    leaveType.getName(),
                    entitlement.getFrom(),
                    entitlement.getTo(),
                    entitlement.getEntitlement(),
                    used,
                    entitlement.getEntitlement().subtract(used),
                    entitlement.getPolicyId(),
                    entitlement.getBaseEntitlementAmount(),
                    entitlement.getCarriedForwardAmount(),
                    entitlement.getAdjustmentAmount(),
                    entitlement.getGeneratedAt()));
        }

        return List.copyOf(results);
    }

    private BigDecimal applicationAmount(LeaveApplication application) {
        return application.getLeaveDuration() == LeaveDuration.FULL ? BigDecimal.ONE : HALF_DAY;
    }

    public record LeaveBalanceResult(
            String leaveTypeId,
            String leaveTypeName,
            LocalDate from,
            LocalDate to,
            BigDecimal entitlement,
            BigDecimal used,
            BigDecimal balance,
            String policyId,
            BigDecimal baseEntitlementAmount,
            BigDecimal carriedForwardAmount,
            BigDecimal adjustmentAmount,
            Instant generatedAt
    ) {
    }
}
