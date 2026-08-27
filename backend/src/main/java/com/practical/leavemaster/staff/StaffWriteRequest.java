package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.leavetype.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Explicit API contract for creating and updating staff.
 *
 * Persistence-only leave type fields are intentionally not accepted from clients. Entitlements
 * identify their leave type by ID and StaffService resolves and tenant-validates that ID.
 */
public record StaffWriteRequest(
        String id,
        String name,
        String email,
        LocalDate joinDate,
        List<WorkScheduleDay> workSchedule,
        LocalDate termDate,
        String jurisdictionId,
        List<EntitlementInput> leaveEntitlements,
        String loginName,
        Set<String> roleIds,
        EmploymentType employmentType,
        List<LeaveApproverInput> leaveApprovers) {

    /** Backward-compatible constructor for existing internal Java callers. */
    public StaffWriteRequest(
            String id,
            String name,
            String email,
            LocalDate joinDate,
            List<WorkScheduleDay> workSchedule,
            LocalDate termDate,
            String jurisdictionId,
            List<EntitlementInput> leaveEntitlements,
            String loginName,
            Set<String> roleIds) {
        this(id, name, email, joinDate, workSchedule, termDate, jurisdictionId,
                leaveEntitlements, loginName, roleIds, null, null);
    }

    /** Backward-compatible constructor retaining employment type support. */
    public StaffWriteRequest(
            String id,
            String name,
            String email,
            LocalDate joinDate,
            List<WorkScheduleDay> workSchedule,
            LocalDate termDate,
            String jurisdictionId,
            List<EntitlementInput> leaveEntitlements,
            String loginName,
            Set<String> roleIds,
            EmploymentType employmentType) {
        this(id, name, email, joinDate, workSchedule, termDate, jurisdictionId,
                leaveEntitlements, loginName, roleIds, employmentType, null);
    }

    public Staff toStaff() {
        return Staff.builder()
                .id(id)
                .name(name)
                .email(email)
                .joinDate(joinDate)
                .workSchedule(workSchedule == null ? new ArrayList<>() : new ArrayList<>(workSchedule))
                .termDate(termDate)
                .jurisdictionId(jurisdictionId)
                .employmentType(employmentType)
                .leaveEntitlements(toEntitlements(leaveEntitlements))
                .loginName(loginName)
                .roleIds(roleIds)
                .build();
    }

    private static List<LeaveEntitlement> toEntitlements(List<EntitlementInput> inputs) {
        if (inputs == null) {
            return null;
        }
        return inputs.stream().map(EntitlementInput::toEntitlement).toList();
    }

    public record LeaveApproverInput(
            String id,
            String approverId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
    }

    public record EntitlementInput(
            String id,
            String leaveTypeId,
            LocalDate from,
            LocalDate to,
            BigDecimal entitlement,
            String policyId,
            BigDecimal baseEntitlementAmount,
            BigDecimal carriedForwardAmount,
            BigDecimal adjustmentAmount) {

        private LeaveEntitlement toEntitlement() {
            LeaveType leaveType = leaveTypeId == null ? null : LeaveType.builder().id(leaveTypeId).build();
            return LeaveEntitlement.builder()
                    .id(id)
                    .leaveType(leaveType)
                    .from(from)
                    .to(to)
                    .entitlement(entitlement)
                    .policyId(policyId)
                    .baseEntitlementAmount(baseEntitlementAmount)
                    .carriedForwardAmount(carriedForwardAmount == null ? BigDecimal.ZERO : carriedForwardAmount)
                    .adjustmentAmount(adjustmentAmount == null ? BigDecimal.ZERO : adjustmentAmount)
                    .build();
        }
    }
}
