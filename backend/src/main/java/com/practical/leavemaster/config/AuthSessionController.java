package com.practical.leavemaster.config;

import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserNotFoundException;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";
    private static final Set<String> LINKABLE_OAUTH_PROVIDERS = Set.of("google", "github");

    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final StaffRepository staffRepository;
    private final JurisdictionRepository jurisdictionRepository;

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

    @GetMapping("/oauth-link/status")
    public ResponseEntity<OAuthLinkStatusResponse> oauthLinkStatus(Authentication authentication) {
        return appUserRepository.findById(authentication.getName())
            .filter(this::isOAuthLinkEligible)
            .map(user -> ResponseEntity.ok(new OAuthLinkStatusResponse(
                user.getOidcProvider() != null && user.getOidcSubject() != null,
                user.getOidcProvider())))
            .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping("/oauth-link/{provider}/start")
    public ResponseEntity<?> startOAuthLink(
        @PathVariable String provider,
        Authentication authentication,
        HttpSession session
    ) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();
        if (!LINKABLE_OAUTH_PROVIDERS.contains(normalizedProvider)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_provider"));
        }

        AppUser user = appUserRepository.findById(authentication.getName()).orElse(null);
        if (!isOAuthLinkEligible(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "account_not_eligible"));
        }
        if (user.getOidcProvider() != null || user.getOidcSubject() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "already_linked"));
        }

        OAuthLinkingContext.create(session, user.getUserId(), normalizedProvider);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/oauth2/authorization/" + normalizedProvider))
            .build();
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

        boolean platformAdmin = hasActivePlatformAdminRole(user);

        String country = Optional.ofNullable(user.getStaffId())
            .flatMap(staffRepository::findById)
            .map(Staff::getJurisdictionId)
            .filter(jurisdictionId -> !jurisdictionId.isBlank())
            .flatMap(jurisdictionRepository::findById)
            .map(jurisdiction -> jurisdiction.getCountryCode())
            .orElse(null);

        return new CurrentUserResponse(
            user.getLoginName(),
            user.getStaffId(),
            user.getTenantId(),
            country,
            user.isActive(),
            platformAdmin,
            authorities
        );
    }

    private boolean isOAuthLinkEligible(AppUser user) {
        return user != null
            && user.isActive()
            && (user.getTenantId() != null || hasActivePlatformAdminRole(user));
    }

    private boolean hasActivePlatformAdminRole(AppUser user) {
        return user != null
            && user.getRoles() != null
            && user.getRoles().stream()
                .anyMatch(role -> role != null
                    && role.isActive()
                    && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
    }

    public record CsrfResponse(String token, String headerName, String parameterName) {
    }

    public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmNewPassword
    ) {
    }

    public record OAuthLinkStatusResponse(boolean linked, String provider) {
    }

    public record CurrentUserResponse(
        String loginName,
        String staffId,
        String tenantId,
        String country,
        boolean active,
        boolean platformAdmin,
        List<String> authorities
    ) {
    }
}
