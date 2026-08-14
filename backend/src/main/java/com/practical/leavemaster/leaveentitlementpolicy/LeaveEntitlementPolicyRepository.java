package com.practical.leavemaster.leaveentitlementpolicy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveEntitlementPolicyRepository extends JpaRepository<LeaveEntitlementPolicy, String> {
    List<LeaveEntitlementPolicy> findAllByTenantId(String tenantId);
    List<LeaveEntitlementPolicy> findAllByTenantIdAndLeaveTypeIdAndActiveTrue(String tenantId, String leaveTypeId);
    boolean existsByLeaveTypeId(String leaveTypeId);
}
