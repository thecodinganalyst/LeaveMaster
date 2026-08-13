package com.practical.leavemaster.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {

    List<Location> findAllByTenantId(String tenantId);

    void deleteAllByTenantId(String tenantId);
}
