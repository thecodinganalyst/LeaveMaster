package com.practical.leavemaster.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, String> {
    List<Staff> findAllByTenantId(String tenantId);

    Optional<Staff> findByIdAndTenantId(String id, String tenantId);
}
