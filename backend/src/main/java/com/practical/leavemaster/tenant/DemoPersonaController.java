package com.practical.leavemaster.tenant;

import com.practical.leavemaster.user.TenantAuthenticationProvider;
import com.practical.leavemaster.user.TenantAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class DemoPersonaController {

    private final TenantAuthenticationProvider tenantAuthenticationProvider;
    private final DemoTenantPolicy demoTenantPolicy;

    @Value("${demo.tenant.id:DEMO}")
    private String demoTenantId;

    @Value("${demo.tenant.password:Demo123!}")
    private String demoPassword;

    @PostMapping("/demo-login")
    public ResponseEntity<DemoLoginResponse> login(@RequestBody DemoLoginRequest request, HttpServletRequest httpRequest) {
        DemoPersona persona = DemoPersona.from(request.persona());
        if (!demoTenantPolicy.isDemoTenant(demoTenantId)) {
            throw new DemoTenantOperationException("public demo login requires a configured DEMO tenant");
        }

        Authentication authentication = tenantAuthenticationProvider.authenticate(
                new TenantAuthenticationToken(demoTenantId, persona.loginName, demoPassword));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok(new DemoLoginResponse(persona.apiName, demoTenantId, persona.loginName));
    }

    public record DemoLoginRequest(String persona) {
    }

    public record DemoLoginResponse(String persona, String tenantId, String loginName) {
    }

    enum DemoPersona {
        EMPLOYEE("employee", "demo.staff"),
        MANAGER("manager", "demo.manager"),
        HR("hr", "demo.hr");

        private final String apiName;
        private final String loginName;

        DemoPersona(String apiName, String loginName) {
            this.apiName = apiName;
            this.loginName = loginName;
        }

        static DemoPersona from(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Demo persona is required");
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return java.util.Arrays.stream(values())
                    .filter(persona -> persona.apiName.equals(normalized))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported demo persona: " + value));
        }
    }
}
