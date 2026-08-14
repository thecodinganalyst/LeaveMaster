package com.practical.leavemaster.leaveentitlementpolicy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveEntitlementPolicyEligibilityRepository extends JpaRepository<LeaveEntitlementPolicyEligibilityRule, String> {
    List<LeaveEntitlementPolicyEligibilityRule> findAllByPolicyIdOrderBySortOrderAsc(String policyId);
    List<LeaveEntitlementPolicyEligibilityRule> findAllByPolicyIdAndActiveTrueOrderBySortOrderAsc(String policyId);
    boolean existsByPolicyId(String policyId);
}
