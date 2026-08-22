package com.practical.leavemaster.leaveentitlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventLeaveEntitlementRepository extends JpaRepository<EventLeaveEntitlement, String> {
    Optional<EventLeaveEntitlement> findByQualifyingEventIdAndPolicyId(String qualifyingEventId, String policyId);
    List<EventLeaveEntitlement> findAllByTenantIdAndStaffIdAndLeaveTypeIdOrderByValidFromAsc(
            String tenantId, String staffId, String leaveTypeId);
}
