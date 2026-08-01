package com.practical.leavemaster.tenant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    List<Tenant> findAllByStatusAndLastModifiedBefore(TenantStatus status, LocalDateTime lastModified);

    @Modifying
    @Query("update Tenant t set t.lastModified = :lastModified where t.id = :tenantId")
    int updateLastModified(@Param("tenantId") String tenantId, @Param("lastModified") LocalDateTime lastModified);
}
