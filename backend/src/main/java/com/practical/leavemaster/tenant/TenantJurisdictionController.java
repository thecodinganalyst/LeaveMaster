package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/tenant-jurisdictions", "/api/tenant-jurisdictions"})
@RequiredArgsConstructor
public class TenantJurisdictionController {

    private final TenantService tenantService;

    @GetMapping
    public List<TenantJurisdiction> getAll(Authentication authentication) {
        return tenantService.findJurisdictionsForUser(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<TenantJurisdiction> create(
            Authentication authentication,
            @RequestBody TenantJurisdictionProvisionRequest request
    ) {
        TenantJurisdiction association = tenantService.addJurisdictionForUser(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(association);
    }
}
