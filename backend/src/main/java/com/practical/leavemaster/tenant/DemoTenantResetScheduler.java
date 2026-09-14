package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoTenantResetScheduler {

    private final DemoTenantSeedService demoTenantSeedService;

    @Value("${demo.tenant.reset-enabled:false}")
    private boolean resetEnabled;

    @Scheduled(cron = "${demo.tenant.reset-cron:0 0 3 * * *}")
    public void resetDemoTenant() {
        if (!resetEnabled) {
            return;
        }
        DemoTenantSeedService.DemoSeedResult result = demoTenantSeedService.resetConfiguredDemoTenant();
        log.info("Reset DEMO tenant {} to baseline dated {}", result.tenantId(), result.baselineDate());
    }
}
