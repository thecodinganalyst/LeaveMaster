package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.rbac.RbacPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/leave-entitlement-generation", "/api/leave-entitlement-generation"})
@RequiredArgsConstructor
public class LeaveEntitlementGenerationController {
    private final LeaveEntitlementGenerationService generationService;

    @PostMapping("/staff")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_GENERATE)")
    public List<EntitlementGenerationResult> generateForStaff(@RequestBody StaffGenerationRequest request) {
        return generationService.generateForStaff(request.staffId(), request.periodStart(), request.periodEnd());
    }

    @PostMapping("/tenant")
    @PreAuthorize("hasAuthority(T(com.practical.leavemaster.rbac.RbacPermissions).LEAVE_ENTITLEMENT_GENERATE)")
    public List<EntitlementGenerationResult> generateForTenant(@RequestBody TenantGenerationRequest request) {
        return generationService.generateForTenant(request.tenantId(), request.periodStart(), request.periodEnd());
    }

    public record StaffGenerationRequest(String staffId, LocalDate periodStart, LocalDate periodEnd) {}
    public record TenantGenerationRequest(String tenantId, LocalDate periodStart, LocalDate periodEnd) {}
}
