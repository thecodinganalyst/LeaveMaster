package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/jurisdictions", "/api/jurisdictions"})
@RequiredArgsConstructor
public class JurisdictionController {
    private static final String PLATFORM_JURISDICTION_WRITE =
            "hasAuthority('JURISDICTION_WRITE') and @platformAdminAccess.isPlatformAdmin(authentication)";

    private final JurisdictionService jurisdictionService;
    private final JurisdictionLeaveTypeService leaveTypeService;

    @GetMapping
    @PreAuthorize("hasAuthority('JURISDICTION_READ')")
    public List<Jurisdiction> getAll() {
        return jurisdictionService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('JURISDICTION_READ')")
    public ResponseEntity<Jurisdiction> getById(@PathVariable String id) {
        return jurisdictionService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/leave-types")
    @PreAuthorize("hasAuthority('JURISDICTION_LEAVE_TYPE_READ')")
    public ResponseEntity<?> getEffectiveLeaveTypes(@PathVariable String id) {
        try {
            return ResponseEntity.ok(leaveTypeService.resolveEffective(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize(PLATFORM_JURISDICTION_WRITE)
    public ResponseEntity<?> create(@RequestBody Jurisdiction jurisdiction) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(jurisdictionService.create(jurisdiction));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize(PLATFORM_JURISDICTION_WRITE)
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Jurisdiction jurisdiction) {
        try {
            return ResponseEntity.ok(jurisdictionService.update(id, jurisdiction));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(PLATFORM_JURISDICTION_WRITE)
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            jurisdictionService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
