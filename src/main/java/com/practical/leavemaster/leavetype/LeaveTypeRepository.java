package com.practical.leavemaster.leavetype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, String> {

    void deleteAllByTenantId(String tenantId);
}
