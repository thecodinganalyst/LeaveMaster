package com.practical.leavemaster.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findById(String loginName);

    Optional<AppUser> findByStaffId(String staffId);

    Optional<AppUser> findByOidcProviderAndOidcSubject(String oidcProvider, String oidcSubject);

    void deleteAllByTenantId(String tenantId);
}
