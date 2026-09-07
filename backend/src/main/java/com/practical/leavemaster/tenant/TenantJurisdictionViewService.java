package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantJurisdictionViewService {

    private final TenantJurisdictionRepository tenantJurisdictionRepository;

    public Tenant enrich(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) return tenant;

        List<String> jurisdictionIds = tenantJurisdictionRepository
                .findAllByTenantIdOrderByJurisdictionIdAsc(tenant.getId())
                .stream()
                .map(TenantJurisdiction::getJurisdictionId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (jurisdictionIds.isEmpty() && tenant.getJurisdictionId() != null && !tenant.getJurisdictionId().isBlank()) {
            jurisdictionIds = List.of(tenant.getJurisdictionId());
        }

        tenant.setJurisdictionIds(jurisdictionIds);
        return tenant;
    }

    public List<Tenant> enrichAll(List<Tenant> tenants) {
        if (tenants == null || tenants.isEmpty()) return tenants;
        tenants.forEach(this::enrich);
        return tenants;
    }
}
