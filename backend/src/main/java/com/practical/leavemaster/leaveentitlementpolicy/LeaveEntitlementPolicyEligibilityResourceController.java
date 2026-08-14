package com.practical.leavemaster.leaveentitlementpolicy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/leave-entitlement-policy-eligibility-rules", "/api/leave-entitlement-policy-eligibility-rules"})
public class LeaveEntitlementPolicyEligibilityResourceController {
    private final LeaveEntitlementPolicyEligibilityService eligibilityService;

    public LeaveEntitlementPolicyEligibilityResourceController(LeaveEntitlementPolicyEligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_READ)")
    public List<LeaveEntitlementPolicyEligibilityRule> getAll() {
        return eligibilityService.findAllAccessible();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_READ)")
    public ResponseEntity<LeaveEntitlementPolicyEligibilityRule> getById(@PathVariable String id) {
        return eligibilityService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public ResponseEntity<LeaveEntitlementPolicyEligibilityRule> create(@RequestBody LeaveEntitlementPolicyEligibilityRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eligibilityService.create(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public LeaveEntitlementPolicyEligibilityRule update(@PathVariable String id, @RequestBody LeaveEntitlementPolicyEligibilityRule rule) {
        return eligibilityService.update(id, rule);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        eligibilityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
