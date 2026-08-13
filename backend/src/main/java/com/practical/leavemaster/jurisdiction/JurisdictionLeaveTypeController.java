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
@RequestMapping({"/jurisdiction-leave-types", "/api/jurisdiction-leave-types"})
@RequiredArgsConstructor
public class JurisdictionLeaveTypeController {
    private final JurisdictionLeaveTypeService leaveTypeService;

    @GetMapping
    @PreAuthorize("hasAuthority('JURISDICTION_LEAVE_TYPE_READ')")
    public List<JurisdictionLeaveType> getAll() {
        return leaveTypeService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('JURISDICTION_LEAVE_TYPE_READ')")
    public ResponseEntity<JurisdictionLeaveType> getById(@PathVariable String id) {
        return leaveTypeService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('JURISDICTION_LEAVE_TYPE_WRITE')")
    public ResponseEntity<?> create(@RequestBody JurisdictionLeaveType leaveType) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(leaveTypeService.create(leaveType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('JURISDICTION_LEAVE_TYPE_WRITE')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody JurisdictionLeaveType leaveType) {
        try {
            return ResponseEntity.ok(leaveTypeService.update(id, leaveType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('JURISDICTION_LEAVE_TYPE_WRITE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            leaveTypeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
