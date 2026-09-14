package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/tenants/demo", "/api/tenants/demo"})
@RequiredArgsConstructor
public class DemoTenantController {

    private final DemoTenantSeedService demoTenantSeedService;

    @PostMapping("/reset")
    public ResponseEntity<DemoTenantSeedService.DemoSeedResult> resetConfiguredDemoTenant() {
        return ResponseEntity.ok(demoTenantSeedService.resetConfiguredDemoTenant());
    }
}
