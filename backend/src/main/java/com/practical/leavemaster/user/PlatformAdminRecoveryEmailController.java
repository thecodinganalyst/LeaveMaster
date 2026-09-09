package com.practical.leavemaster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/platform-admin", "/api/platform-admin"})
@RequiredArgsConstructor
public class PlatformAdminRecoveryEmailController {

    private final AppUserRepository appUserRepository;

    @PutMapping("/recovery-email")
    public ResponseEntity<?> updateRecoveryEmail(Authentication authentication, @RequestBody Map<String, String> body) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = body.get("email");
        if (email == null || email.isBlank() || !isValidEmail(email.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid recovery email address"));
        }

        AppUser user = appUserRepository.findById(authentication.getName()).orElse(null);
        if (user == null || user.getTenantId() != null || !hasPlatformAdminRole(user)) {
            return ResponseEntity.status(403).build();
        }

        user.setEmail(email.trim());
        appUserRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private boolean hasPlatformAdminRole(AppUser user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role != null && role.isActive()
                        && "PLATFORM_ADMIN".equalsIgnoreCase(role.getId()));
    }

    private boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1 && email.indexOf(' ', 0) < 0;
    }
}
