package com.practicallimits.spring_template.leaveapplication;

import com.practicallimits.spring_template.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, String> {
    List<LeaveApplication> findByStaff(Staff staff);
}
