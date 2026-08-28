package com.practical.leavemaster.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findById(String userId);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    List<AppUser> findAllByLoginName(String loginName);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByTenantIdAndLoginName(String tenantId, String loginName);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByTenantIdIsNullAndLoginName(String loginName);

    boolean existsByTenantIdAndLoginName(String tenantId, String loginName);

    boolean existsByTenantIdIsNullAndLoginName(String loginName);

    Optional<AppUser> findByTenantIdAndStaffId(String tenantId, String staffId);

    List<AppUser> findAllByStaffId(String staffId);

    Optional<AppUser> findByOidcProviderAndOidcSubject(String oidcProvider, String oidcSubject);

    void deleteAllByTenantId(String tenantId);

    default Optional<AppUser> findUniqueByLoginName(String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return Optional.empty();
        }
        List<AppUser> matches = findAllByLoginName(loginName.trim());
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    default Optional<AppUser> findUniqueByStaffId(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            return Optional.empty();
        }
        List<AppUser> matches = findAllByStaffId(staffId.trim());
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    default Optional<AppUser> findScopedByLoginName(String tenantId, String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return Optional.empty();
        }
        String normalizedLoginName = loginName.trim();
        return tenantId == null
                ? findByTenantIdIsNullAndLoginName(normalizedLoginName)
                : findByTenantIdAndLoginName(tenantId, normalizedLoginName);
    }

    default boolean existsScopedLoginName(String tenantId, String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return false;
        }
        String normalizedLoginName = loginName.trim();
        return tenantId == null
                ? existsByTenantIdIsNullAndLoginName(normalizedLoginName)
                : existsByTenantIdAndLoginName(tenantId, normalizedLoginName);
    }
}
