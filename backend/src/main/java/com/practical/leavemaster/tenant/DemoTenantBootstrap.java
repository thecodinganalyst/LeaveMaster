package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoTenantBootstrap {

    private final DemoTenantBootstrapService demoTenantBootstrapService;

    @Value("${demo.tenant.bootstrap-enabled:false}")
    private boolean bootstrapEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapDemoTenant() {
        if (!bootstrapEnabled) {
            return;
        }

        DemoTenantBootstrapService.DemoBootstrapResult result =
                demoTenantBootstrapService.ensureConfiguredDemoTenantReady();
        log.info("DEMO tenant {} startup bootstrap result: {}", result.tenantId(), result.action());
    }
}
