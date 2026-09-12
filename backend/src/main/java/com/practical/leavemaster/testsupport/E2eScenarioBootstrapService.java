package com.practical.leavemaster.testsupport;

import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.tenant.TenantJurisdiction;
import com.practical.leavemaster.tenant.TenantRepository;
import com.practical.leavemaster.tenant.TenantService;
import com.practical.leavemaster.user.AppUser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@Profile("e2e")
@RequiredArgsConstructor
public class E2eScenarioBootstrapService {

    static final String TENANT_PREFIX = "E2E-";
    static final String DEFAULT_PASSWORD = "e2e-password";

    private static final Set<String> STAFF_PERMISSIONS = Set.of(
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_CALENDAR_READ
    );
    private static final Set<String> MANAGER_PERMISSIONS = Set.of(
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_CALENDAR_READ
    );
    private static final Set<String> HR_PERMISSIONS = Set.of(
            RbacPermissions.USER_READ,
            RbacPermissions.USER_WRITE,
            RbacPermissions.STAFF_READ,
            RbacPermissions.STAFF_WRITE,
            RbacPermissions.JURISDICTION_READ,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_TYPE_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_GENERATE,
            RbacPermissions.LEAVE_APPROVER_READ,
            RbacPermissions.LEAVE_APPROVER_WRITE,
            RbacPermissions.LEAVE_CALENDAR_READ,
            RbacPermissions.LEAVE_CALENDAR_WRITE,
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE
    );
    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            RbacPermissions.USER_READ,
            RbacPermissions.USER_WRITE,
            RbacPermissions.ROLE_MANAGE,
            RbacPermissions.STAFF_READ,
            RbacPermissions.STAFF_WRITE,
            RbacPermissions.JURISDICTION_READ,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_TYPE_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_GENERATE,
            RbacPermissions.LEAVE_APPROVER_READ,
            RbacPermissions.LEAVE_APPROVER_WRITE,
            RbacPermissions.LEAVE_CALENDAR_READ,
            RbacPermissions.LEAVE_CALENDAR_WRITE,
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE
    );

    private final TenantRepository tenantRepository;
    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional
    public ScenarioBootstrapResult createStandardSingaporeScenario(String requestedScenarioId, LocalDate referenceDate) {
        String scenarioId = ScenarioDataFactory.normalizeScenarioId(requestedScenarioId);
        ScenarioDataFactory.Scenario scenario = ScenarioDataFactory.standardSingaporeScenario(scenarioId, referenceDate);
        String tenantId = scenario.tenant().getId();

        if (!tenantId.startsWith(TENANT_PREFIX)) {
            throw new IllegalArgumentException("E2E scenarios must use the reserved E2E tenant prefix");
        }
        if (tenantRepository.existsById(tenantId)) {
            throw new ScenarioAlreadyExistsException(scenarioId);
        }

        // Browser-driven E2E tests authenticate through the real security stack, so the seeded
        // roles must carry the same permission sets as production tenant roles.
        applyPermissions(scenario.roles().get("staff"), STAFF_PERMISSIONS);
        applyPermissions(scenario.roles().get("manager"), MANAGER_PERMISSIONS);
        applyPermissions(scenario.roles().get("hr"), HR_PERMISSIONS);
        applyPermissions(scenario.roles().get("admin"), ADMIN_PERMISSIONS);

        // The in-memory factory can use descriptive IDs/source metadata, but persistence must
        // respect the production entity mappings and foreign keys. Entitlements and approvers use
        // generated UUIDs, and this baseline scenario does not create entitlement-policy rows.
        scenario.entitlements().values().forEach(entitlement -> {
            entitlement.setId(null);
            entitlement.setPolicyId(null);
        });
        scenario.approvers().forEach(approver -> approver.setId(null));

        // Assigned-ID scenario entities are known to be new. Persist them explicitly rather than
        // using Spring Data save(), whose merge semantics can treat assigned IDs as detached rows.
        entityManager.persist(scenario.tenant());
        entityManager.persist(TenantJurisdiction.builder()
                .id(TenantJurisdiction.idFor(tenantId, scenario.tenant().getJurisdictionId()))
                .tenantId(tenantId)
                .jurisdictionId(scenario.tenant().getJurisdictionId())
                .build());
        scenario.roles().values().forEach(entityManager::persist);
        scenario.leaveTypes().forEach(entityManager::persist);
        scenario.staff().values().forEach(entityManager::persist);
        scenario.dependants().forEach(entityManager::persist);
        scenario.approvers().forEach(entityManager::persist);

        scenario.users().values().forEach(user -> {
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            entityManager.persist(user);
        });
        entityManager.flush();

        Map<String, ScenarioUser> users = new LinkedHashMap<>();
        scenario.users().forEach((alias, user) -> users.put(alias, toScenarioUser(alias, user)));

        return new ScenarioBootstrapResult(
                scenario.scenarioId(),
                tenantId,
                scenario.tenant().getJurisdictionId(),
                scenario.referenceDate(),
                DEFAULT_PASSWORD,
                users);
    }

    private void applyPermissions(AppRole role, Set<String> permissionCodes) {
        if (role != null) {
            role.setPermissions(Set.copyOf(appPermissionRepository.findAllById(permissionCodes)));
        }
    }

    @Transactional
    public void deleteScenario(String requestedScenarioId) {
        String scenarioId = ScenarioDataFactory.normalizeScenarioId(requestedScenarioId);
        String tenantId = TENANT_PREFIX + scenarioId;
        requireE2eTenant(tenantId);
        if (!tenantRepository.existsById(tenantId)) {
            return;
        }

        tenantService.delete(tenantId);
        // Scenario roles are created by the reusable factory rather than the normal tenant
        // provisioning path, so remove any that remain after tenant deprovisioning.
        appRoleRepository.deleteAllByTenantId(tenantId);
    }

    private static ScenarioUser toScenarioUser(String alias, AppUser user) {
        return new ScenarioUser(alias, user.getLoginName(), user.getStaffId(), user.getEmail());
    }

    private static void requireE2eTenant(String tenantId) {
        if (tenantId == null || !tenantId.startsWith(TENANT_PREFIX)) {
            throw new IllegalArgumentException("Only E2E scenario tenants can be reset");
        }
    }

    public record ScenarioBootstrapResult(
            String scenarioId,
            String tenantId,
            String jurisdictionId,
            LocalDate referenceDate,
            String password,
            Map<String, ScenarioUser> users) {
    }

    public record ScenarioUser(String alias, String loginName, String staffId, String email) {
    }

    public static class ScenarioAlreadyExistsException extends RuntimeException {
        public ScenarioAlreadyExistsException(String scenarioId) {
            super("Scenario already exists: " + scenarioId);
        }
    }
}
