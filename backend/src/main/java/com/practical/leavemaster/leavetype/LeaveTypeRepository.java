package com.practical.leavemaster.leavetype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, String> {

    List<LeaveType> findAllByTenantId(String tenantId);

    void deleteAllByTenantId(String tenantId);
}
