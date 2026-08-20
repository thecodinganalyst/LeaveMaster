package com.practical.leavemaster.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantJurisdictionRepository extends JpaRepository<TenantJurisdiction, String> {
    List<TenantJurisdiction> findAllByTenantIdOrderByJurisdictionIdAsc(String tenantId);
    boolean existsByTenantIdAndJurisdictionId(String tenantId, String jurisdictionId);
    void deleteAllByTenantId(String tenantId);
}
