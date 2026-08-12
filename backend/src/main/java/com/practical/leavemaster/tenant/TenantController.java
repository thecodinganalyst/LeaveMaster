package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/tenants", "/api/tenants"})
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public List<Tenant> getAll() {
        return tenantService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getById(@PathVariable String id) {
        return tenantService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tenant> create(@RequestBody Tenant tenant) {
        Tenant saved = tenantService.save(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> update(@PathVariable String id, @RequestBody Tenant tenant) {
        try {
            Tenant updated = tenantService.update(id, tenant);
            return ResponseEntity.ok(updated);
        } catch (TenantNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            tenantService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (TenantNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
