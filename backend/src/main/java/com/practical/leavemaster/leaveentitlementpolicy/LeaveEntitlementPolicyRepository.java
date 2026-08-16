package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveEntitlementPolicyRepository extends JpaRepository<LeaveEntitlementPolicy, String> {
    List<LeaveEntitlementPolicy> findAllByTenantId(String tenantId);
    List<LeaveEntitlementPolicy> findAllByScope(ConfigurationScope scope);
    List<LeaveEntitlementPolicy> findAllByScopeAndJurisdictionIdAndActiveTrue(ConfigurationScope scope, String jurisdictionId);
    List<LeaveEntitlementPolicy> findAllByTenantIdAndLeaveTypeIdAndActiveTrue(String tenantId, String leaveTypeId);
    boolean existsByLeaveTypeId(String leaveTypeId);
    boolean existsByTenantIdAndSourceTemplateId(String tenantId, String sourceTemplateId);
    void deleteAllByTenantId(String tenantId);
}
