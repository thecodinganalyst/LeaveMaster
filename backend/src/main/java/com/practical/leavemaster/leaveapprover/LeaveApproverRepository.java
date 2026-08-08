package com.practical.leavemaster.leaveapprover;

import com.practical.leavemaster.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveApproverRepository extends JpaRepository<LeaveApprover, String> {
    List<LeaveApprover> findByStaff(Staff staff);

    @Query("SELECT la FROM LeaveApprover la WHERE la.staff = :staff AND la.effectiveFrom <= :date AND (la.effectiveTo IS NULL OR la.effectiveTo >= :date)")
    List<LeaveApprover> findActiveApproversForStaff(@Param("staff") Staff staff, @Param("date") LocalDate date);

    List<LeaveApprover> findByApprover(Staff approver);

    void deleteAllByTenantId(String tenantId);
}
