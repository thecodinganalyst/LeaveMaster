package com.practical.leavemaster.tenant;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoTenantBootstrapService {

    private static final List<String> REQUIRED_PERSONA_LOGINS = List.of(
            "demo.staff",
            "demo.manager",
            "demo.hr"
    );

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final DemoTenantSeedService demoTenantSeedService;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.tenant.id:DEMO}")
    private String configuredTenantId;

    @Value("${demo.tenant.password:Demo123!}")
    private String demoPassword;

    public DemoBootstrapResult ensureConfiguredDemoTenantReady() {
        String tenantId = normalizeTenantId(configuredTenantId);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);

        if (tenant == null) {
            DemoTenantSeedService.DemoSeedResult seedResult = demoTenantSeedService.resetConfiguredDemoTenant();
            return new DemoBootstrapResult(seedResult.tenantId(), DemoBootstrapAction.CREATED);
        }

        if (tenant.getType() != TenantType.DEMO) {
            throw new DemoTenantOperationException("bootstrap non-DEMO tenant " + tenantId);
        }

        boolean unusableRequiredPersona = REQUIRED_PERSONA_LOGINS.stream()
                .anyMatch(loginName -> !isPersonaLoginUsable(tenantId, loginName));
        if (unusableRequiredPersona) {
            DemoTenantSeedService.DemoSeedResult seedResult = demoTenantSeedService.resetExistingDemoTenant(tenantId);
            return new DemoBootstrapResult(seedResult.tenantId(), DemoBootstrapAction.REPAIRED);
        }

        return new DemoBootstrapResult(tenantId, DemoBootstrapAction.ALREADY_READY);
    }

    private boolean isPersonaLoginUsable(String tenantId, String loginName) {
        AppUser user = appUserRepository.findByTenantIdAndLoginName(tenantId, loginName).orElse(null);
        return user != null
                && user.isActive()
                && user.getPassword() != null
                && passwordEncoder.matches(demoPassword, user.getPassword());
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Demo tenant id must not be blank");
        }
        return tenantId.trim();
    }

    public enum DemoBootstrapAction {
        CREATED,
        REPAIRED,
        ALREADY_READY
    }

    public record DemoBootstrapResult(String tenantId, DemoBootstrapAction action) {
    }
}
