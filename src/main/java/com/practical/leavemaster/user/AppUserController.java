package com.practical.leavemaster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @GetMapping
    public List<AppUser> getAll() {
        return appUserService.findAll();
    }

    @GetMapping("/{loginName}")
    public ResponseEntity<AppUser> getByLoginName(@PathVariable String loginName) {
        return appUserService.findByLoginName(loginName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AppUser user) {
        try {
            AppUser saved = appUserService.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (DuplicateLoginNameException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{loginName}")
    public ResponseEntity<?> update(@PathVariable String loginName, @RequestBody AppUser user) {
        try {
            AppUser updated = appUserService.update(loginName, user);
            return ResponseEntity.ok(updated);
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{loginName}")
    public ResponseEntity<Void> delete(@PathVariable String loginName) {
        try {
            appUserService.delete(loginName);
            return ResponseEntity.noContent().build();
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{loginName}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable String loginName, @RequestBody Map<String, String> body) {
        try {
            AppUser updated = appUserService.changePassword(loginName, body.get("password"));
            return ResponseEntity.ok(updated);
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{loginName}/activate")
    public ResponseEntity<?> activate(@PathVariable String loginName) {
        try {
            AppUser updated = appUserService.activate(loginName);
            return ResponseEntity.ok(updated);
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{loginName}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable String loginName) {
        try {
            AppUser updated = appUserService.deactivate(loginName);
            return ResponseEntity.ok(updated);
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            AppUser user = appUserService.login(body.get("loginName"), body.get("password"));
            return ResponseEntity.ok(user);
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}
