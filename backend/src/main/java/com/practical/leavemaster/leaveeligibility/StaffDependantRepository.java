package com.practical.leavemaster.leaveeligibility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffDependantRepository extends JpaRepository<StaffDependant, String> {
    List<StaffDependant> findAllByStaffId(String staffId);
    List<StaffDependant> findAllByTenantIdAndStaffId(String tenantId, String staffId);
}
