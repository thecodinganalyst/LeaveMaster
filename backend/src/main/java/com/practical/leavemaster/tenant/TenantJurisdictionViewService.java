package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantJurisdictionViewService {

    private final TenantJurisdictionRepository tenantJurisdictionRepository;

    public Tenant enrich(Tenant tenant) {
        List<String> jurisdictionIds = tenantJurisdictionRepository
                .findAllByTenantIdOrderByJurisdictionIdAsc(tenant.getId())
                .stream()
                .map(TenantJurisdiction::getJurisdictionId)
                .toList();

        tenant.setJurisdictionIds(jurisdictionIds.isEmpty()
                ? List.of(tenant.getJurisdictionId())
                : jurisdictionIds);
        return tenant;
    }

    public List<Tenant> enrichAll(List<Tenant> tenants) {
        tenants.forEach(this::enrich);
        return tenants;
    }
}
