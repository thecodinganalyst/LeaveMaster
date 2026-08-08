package com.practical.leavemaster.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppPermissionRepository extends JpaRepository<AppPermission, String> {
}
