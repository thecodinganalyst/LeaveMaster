package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
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
@RequestMapping({"/leave-entitlement-policies", "/api/leave-entitlement-policies"})
@RequiredArgsConstructor
public class LeaveEntitlementPolicyController {
    private final LeaveEntitlementPolicyService policyService;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_READ)")
    public List<LeaveEntitlementPolicy> getAll() {
        return policyService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_READ)")
    public ResponseEntity<LeaveEntitlementPolicy> getById(@PathVariable String id) {
        return policyService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public ResponseEntity<LeaveEntitlementPolicy> create(@RequestBody LeaveEntitlementPolicy policy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(policy));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public LeaveEntitlementPolicy update(@PathVariable String id, @RequestBody LeaveEntitlementPolicy policy) {
        return policyService.update(id, policy);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_POLICY_WRITE)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
