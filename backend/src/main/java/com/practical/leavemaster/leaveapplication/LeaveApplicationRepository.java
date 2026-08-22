package com.practical.leavemaster.leaveapplication;

import com.practical.leavemaster.leaveapprover.LeaveApprover;
import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, String> {
    List<LeaveApplication> findByStaff(Staff staff);

    boolean existsByStaffId(String staffId);

    boolean existsByApproverId(String approverId);

    List<LeaveApplication> findByStaffAndLeaveDateBetween(Staff staff, LocalDate from, LocalDate to);

    List<LeaveApplication> findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
            Staff staff, LeaveType leaveType, LocalDate from, LocalDate to, List<LeaveStatus> statuses);

    List<LeaveApplication> findAllByEventEntitlementIdAndStatus(String eventEntitlementId, LeaveStatus status);

    @Query("SELECT la FROM LeaveApplication la " +
           "JOIN LeaveApprover lap ON lap.staff = la.staff " +
           "WHERE lap.approver.id = :approverId " +
           "AND la.status = LeaveStatus.PENDING " +
           "AND la.leaveDate >= lap.effectiveFrom " +
           "AND (lap.effectiveTo IS NULL OR la.leaveDate <= lap.effectiveTo)")
    List<LeaveApplication> findPendingByApproverId(@Param("approverId") String approverId);

    @Query("SELECT DISTINCT la FROM LeaveApplication la " +
           "WHERE la.staff.id = :staffId " +
           "OR EXISTS (" +
           "    SELECT lap FROM LeaveApprover lap " +
           "    WHERE lap.approver.id = :staffId " +
           "    AND lap.staff = la.staff " +
           "    AND la.leaveDate >= lap.effectiveFrom " +
           "    AND (lap.effectiveTo IS NULL OR la.leaveDate <= lap.effectiveTo)" +
           ")")
    List<LeaveApplication> findVisibleForStaff(@Param("staffId") String staffId);

    void deleteAllByTenantId(String tenantId);
}
