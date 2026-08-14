package com.practical.leavemaster.leaveentitlementpolicy;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/leave-entitlement-policies", "/api/leave-entitlement-policies"})
public class LeaveEntitlementPolicyEligibilityController {
    private final LeaveEntitlementPolicyEligibilityService eligibilityService;
    private final LeaveEntitlementPolicyResolutionService resolutionService;

    public LeaveEntitlementPolicyEligibilityController(
            LeaveEntitlementPolicyEligibilityService eligibilityService,
            LeaveEntitlementPolicyResolutionService resolutionService) {
        this.eligibilityService = eligibilityService;
        this.resolutionService = resolutionService;
    }

    @GetMapping("/{policyId}/eligibility-rules")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_READ)")
    public List<LeaveEntitlementPolicyEligibilityRule> getRules(@PathVariable String policyId) {
        return eligibilityService.findAll(policyId);
    }

    @PostMapping("/{policyId}/eligibility-rules")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public ResponseEntity<LeaveEntitlementPolicyEligibilityRule> createRule(
            @PathVariable String policyId,
            @RequestBody LeaveEntitlementPolicyEligibilityRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eligibilityService.create(policyId, rule));
    }

    @PutMapping("/{policyId}/eligibility-rules/{ruleId}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public LeaveEntitlementPolicyEligibilityRule updateRule(
            @PathVariable String policyId,
            @PathVariable String ruleId,
            @RequestBody LeaveEntitlementPolicyEligibilityRule rule) {
        return eligibilityService.update(policyId, ruleId, rule);
    }

    @DeleteMapping("/{policyId}/eligibility-rules/{ruleId}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public ResponseEntity<Void> deleteRule(@PathVariable String policyId, @PathVariable String ruleId) {
        eligibilityService.delete(policyId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_READ)")
    public PolicyResolutionResult resolve(
            @RequestParam String staffId,
            @RequestParam String leaveTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
        return resolutionService.resolve(staffId, leaveTypeId, effectiveDate);
    }
}
