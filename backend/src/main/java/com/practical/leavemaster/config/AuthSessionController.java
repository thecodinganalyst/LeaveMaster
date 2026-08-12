package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserNotFoundException;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName());
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(Authentication authentication) {
        return appUserRepository.findById(authentication.getName())
            .map(user -> ResponseEntity.ok(toResponse(user, authentication)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
        Authentication authentication,
        @RequestBody ChangePasswordRequest request
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password change request is required"));
        }
        if (request.confirmNewPassword() == null || !request.confirmNewPassword().equals(request.newPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation must match"));
        }

        try {
            appUserService.changeOwnPassword(authentication.getName(), request.currentPassword(), request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (AppUserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private CurrentUserResponse toResponse(AppUser user, Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .sorted(Comparator.naturalOrder())
            .toList();

        return new CurrentUserResponse(
            user.getLoginName(),
            user.getStaffId(),
            user.getTenantId(),
            user.isActive(),
            authorities
        );
    }

    public record CsrfResponse(String token, String headerName, String parameterName) {
    }

    public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmNewPassword
    ) {
    }

    public record CurrentUserResponse(
        String loginName,
        String staffId,
        String tenantId,
        boolean active,
        List<String> authorities
    ) {
    }
}
