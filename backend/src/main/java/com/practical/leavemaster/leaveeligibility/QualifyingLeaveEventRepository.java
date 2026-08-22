package com.practical.leavemaster.leaveeligibility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualifyingLeaveEventRepository extends JpaRepository<QualifyingLeaveEvent, String> {
    List<QualifyingLeaveEvent> findAllByStaffId(String staffId);
    List<QualifyingLeaveEvent> findAllByTenantIdAndStaffId(String tenantId, String staffId);
    boolean existsByDependantId(String dependantId);
    void deleteAllByTenantId(String tenantId);
}
