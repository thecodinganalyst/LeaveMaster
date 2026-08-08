package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TenantDormancyScheduler {

    private final TenantService tenantService;

    @Scheduled(cron = "${tenant.dormancy.cron:0 0 0 * * *}")
    public void markDormantTenants() {
        tenantService.markDormantTenants(LocalDateTime.now().minusMonths(1));
    }
}
