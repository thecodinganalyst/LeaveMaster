package com.practical.leavemaster.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {

    boolean existsByLocationId(String locationId);

    List<Staff> findAllByTenantId(String tenantId);
}
