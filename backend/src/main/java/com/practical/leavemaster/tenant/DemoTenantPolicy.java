package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoTenantPolicy {

    private final TenantRepository tenantRepository;

    public TenantType tenantType(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return TenantType.STANDARD;
        }
        return tenantRepository.findById(tenantId)
                .map(Tenant::getType)
                .orElse(TenantType.STANDARD);
    }

    public boolean isDemoTenant(String tenantId) {
        return tenantType(tenantId) == TenantType.DEMO;
    }

    public boolean suppressOutboundSideEffects(String tenantId, String sideEffect) {
        boolean suppressed = isDemoTenant(tenantId);
        if (suppressed) {
            log.info("Suppressed outbound side effect '{}' for DEMO tenant {}", sideEffect, tenantId);
        }
        return suppressed;
    }

    public void requireStandardTenant(String tenantId, String operation) {
        if (isDemoTenant(tenantId)) {
            throw new DemoTenantOperationException(operation);
        }
    }
}
