package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.leavetype.LeaveType;
import com.practicallimits.spring_template.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, String> {
    List<LeaveApplication> findByStaff(Staff staff);

    List<LeaveApplication> findByStaffAndLeaveDateBetween(Staff staff, LocalDate from, LocalDate to);

    List<LeaveApplication> findByStaffAndLeaveTypeAndLeaveDateBetweenAndStatusIn(
            Staff staff, LeaveType leaveType, LocalDate from, LocalDate to, List<LeaveStatus> statuses);

    @Query("SELECT la FROM LeaveApplication la " +
           "JOIN com.practicallimits.spring_template.leaveapprover.LeaveApprover lap ON lap.staff = la.staff " +
           "WHERE lap.approver.id = :approverId " +
           "AND la.status = com.practicallimits.spring_template.leaveapplication.LeaveStatus.PENDING " +
           "AND la.leaveDate >= lap.effectiveFrom " +
           "AND (lap.effectiveTo IS NULL OR la.leaveDate <= lap.effectiveTo)")
    List<LeaveApplication> findPendingByApproverId(@Param("approverId") String approverId);
}
