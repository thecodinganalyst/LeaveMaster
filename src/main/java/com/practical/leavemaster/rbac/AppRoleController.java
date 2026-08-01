package com.practical.leavemaster.rbac;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class AppRoleController {

    private final AppRoleService appRoleService;

    @GetMapping
    public List<AppRole> getAll() {
        return appRoleService.findAll();
    }

    @GetMapping("/permissions")
    public List<AppPermission> getAllPermissions() {
        return appRoleService.findAllPermissions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppRole> getById(@PathVariable String id) {
        return appRoleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RoleRequest request) {
        try {
            AppRole created = appRoleService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody RoleRequest request) {
        try {
            AppRole updated = appRoleService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable String id) {
        try {
            return ResponseEntity.ok(appRoleService.disable(id));
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enable(@PathVariable String id) {
        try {
            return ResponseEntity.ok(appRoleService.enable(id));
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/users/{loginName}")
    public ResponseEntity<?> addUserToRole(@PathVariable String id, @PathVariable String loginName) {
        try {
            AppUser user = appRoleService.addUserToRole(id, loginName);
            return ResponseEntity.ok(user);
        } catch (RoleNotFoundException | AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RoleDisabledException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/users/{loginName}")
    public ResponseEntity<?> removeUserFromRole(@PathVariable String id, @PathVariable String loginName) {
        try {
            AppUser user = appRoleService.removeUserFromRole(id, loginName);
            return ResponseEntity.ok(user);
        } catch (RoleNotFoundException | AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
