package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.leavetype.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveEntitlementRepository extends JpaRepository<LeaveEntitlement, String> {
    Optional<LeaveEntitlement> findByStaffAndLeaveTypeAndFromAndTo(Staff staff, LeaveType leaveType, LocalDate from, LocalDate to);
    List<LeaveEntitlement> findAllByStaffAndLeaveTypeAndToBeforeOrderByToDesc(Staff staff, LeaveType leaveType, LocalDate before);
    List<LeaveEntitlement> findAllByTenantId(String tenantId);
}
