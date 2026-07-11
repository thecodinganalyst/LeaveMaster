package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApproverRepository extends JpaRepository<LeaveApprover, String> {
    List<LeaveApprover> findByStaff(Staff staff);
}
