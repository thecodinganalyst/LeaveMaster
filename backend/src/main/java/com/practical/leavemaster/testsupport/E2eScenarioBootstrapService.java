package com.practical.leavemaster.testsupport;

import com.practical.leavemaster.rbac.AppRoleRepository;
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

@Service
@Profile("e2e")
@RequiredArgsConstructor
public class E2eScenarioBootstrapService {

    static final String TENANT_PREFIX = "E2E-";
    static final String DEFAULT_PASSWORD = "e2e-password";

    private final TenantRepository tenantRepository;
    private final AppRoleRepository appRoleRepository;
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

        // The factory intentionally assigns deterministic string IDs. Spring Data save() treats
        // assigned-ID entities as candidates for merge, while these scenario objects are known to
        // be new. Persist them explicitly in dependency order to avoid stale-state merge handling.
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
