package com.practical.leavemaster.config;

import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.TenantAuthenticationFilter;
import com.practical.leavemaster.user.TenantAuthenticationProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnBean(HttpSecurity.class)
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
        AppUserRepository appUserRepository,
        AuthenticationManager authenticationManager,
        @Value("${app.public-url:http://localhost:5173}") String publicAppUrl
    ) throws Exception {
        String normalizedPublicAppUrl = stripTrailingSlash(publicAppUrl);

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/mcp/**", "/api/public/contact"))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/users/login",
                    "/api/users/login",
                    "/account-activation/**",
                    "/api/account-activation/**",
                    "/auth/csrf",
                    "/auth/login",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/h2-console/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/contact").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/**", "/api/users/**").hasAuthority(RbacPermissions.USER_READ)
                .requestMatchers(HttpMethod.POST, "/users/**", "/api/users/**").hasAuthority(RbacPermissions.USER_WRITE)
                .requestMatchers(HttpMethod.PUT, "/users/**", "/api/users/**").hasAuthority(RbacPermissions.USER_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/users/**", "/api/users/**").hasAuthority(RbacPermissions.USER_WRITE)
                .requestMatchers("/roles/**", "/api/roles/**").hasAuthority(RbacPermissions.ROLE_MANAGE)
                .requestMatchers(HttpMethod.GET, "/tenants/**", "/api/tenants/**").hasAuthority(RbacPermissions.TENANT_READ)
                .requestMatchers(HttpMethod.POST, "/tenants/**", "/api/tenants/**").hasAuthority(RbacPermissions.TENANT_WRITE)
                .requestMatchers(HttpMethod.PUT, "/tenants/**", "/api/tenants/**").hasAuthority(RbacPermissions.TENANT_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/tenants/**", "/api/tenants/**").hasAuthority(RbacPermissions.TENANT_WRITE)
                .requestMatchers(HttpMethod.GET, "/tenant-jurisdictions/**", "/api/tenant-jurisdictions/**").hasAuthority(RbacPermissions.LEAVE_CALENDAR_READ)
                .requestMatchers(HttpMethod.POST, "/tenant-jurisdictions/**", "/api/tenant-jurisdictions/**").hasAuthority(RbacPermissions.LEAVE_CALENDAR_WRITE)
                .requestMatchers(HttpMethod.GET, "/staff/**", "/api/staff/**").hasAuthority(RbacPermissions.STAFF_READ)
                .requestMatchers(HttpMethod.POST, "/staff/**", "/api/staff/**").hasAuthority(RbacPermissions.STAFF_WRITE)
                .requestMatchers(HttpMethod.PUT, "/staff/**", "/api/staff/**").hasAuthority(RbacPermissions.STAFF_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/staff/**", "/api/staff/**").hasAuthority(RbacPermissions.STAFF_WRITE)
                .requestMatchers(HttpMethod.GET, "/leave-types/**", "/api/leave-types/**").hasAuthority(RbacPermissions.LEAVE_TYPE_READ)
                .requestMatchers(HttpMethod.POST, "/leave-types/**", "/api/leave-types/**").hasAuthority(RbacPermissions.LEAVE_TYPE_WRITE)
                .requestMatchers(HttpMethod.PUT, "/leave-types/**", "/api/leave-types/**").hasAuthority(RbacPermissions.LEAVE_TYPE_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/leave-types/**", "/api/leave-types/**").hasAuthority(RbacPermissions.LEAVE_TYPE_WRITE)
                .requestMatchers(HttpMethod.GET, "/leave-approvers/**", "/api/leave-approvers/**").hasAuthority(RbacPermissions.LEAVE_APPROVER_READ)
                .requestMatchers(HttpMethod.POST, "/leave-approvers/**", "/api/leave-approvers/**").hasAuthority(RbacPermissions.LEAVE_APPROVER_WRITE)
                .requestMatchers(HttpMethod.PUT, "/leave-approvers/**", "/api/leave-approvers/**").hasAuthority(RbacPermissions.LEAVE_APPROVER_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/leave-approvers/**", "/api/leave-approvers/**").hasAuthority(RbacPermissions.LEAVE_APPROVER_WRITE)
                .requestMatchers(HttpMethod.GET, "/public-holidays/**", "/api/public-holidays/**").hasAuthority(RbacPermissions.PUBLIC_HOLIDAY_READ)
                .requestMatchers(HttpMethod.POST, "/public-holidays/**", "/api/public-holidays/**").hasAuthority(RbacPermissions.PUBLIC_HOLIDAY_WRITE)
                .requestMatchers(HttpMethod.PUT, "/public-holidays/**", "/api/public-holidays/**").hasAuthority(RbacPermissions.PUBLIC_HOLIDAY_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/public-holidays/**", "/api/public-holidays/**").hasAuthority(RbacPermissions.PUBLIC_HOLIDAY_WRITE)
                .requestMatchers(HttpMethod.GET, "/leave-calendars/**", "/api/leave-calendars/**").hasAuthority(RbacPermissions.LEAVE_CALENDAR_READ)
                .requestMatchers(HttpMethod.POST, "/leave-calendars/**", "/api/leave-calendars/**").hasAuthority(RbacPermissions.LEAVE_CALENDAR_WRITE)
                .requestMatchers(HttpMethod.PUT, "/leave-calendars/**", "/api/leave-calendars/**").hasAuthority(RbacPermissions.LEAVE_CALENDAR_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/leave-calendars/**", "/api/leave-calendars/**").hasAuthority(RbacPermissions.LEAVE_CALENDAR_WRITE)
                .requestMatchers(HttpMethod.GET, "/leave-applications/**").hasAuthority(RbacPermissions.LEAVE_APPLICATION_READ)
                .requestMatchers(HttpMethod.POST, "/leave-applications/**").hasAuthority(RbacPermissions.LEAVE_APPLICATION_WRITE)
                .requestMatchers(HttpMethod.PUT, "/leave-applications/*/approve").hasAuthority(RbacPermissions.LEAVE_APPLICATION_APPROVE)
                .requestMatchers(HttpMethod.PUT, "/leave-applications/*/reject").hasAuthority(RbacPermissions.LEAVE_APPLICATION_APPROVE)
                .requestMatchers(HttpMethod.PUT, "/leave-applications/*/approve-cancellation").hasAuthority(RbacPermissions.LEAVE_APPLICATION_APPROVE)
                .requestMatchers(HttpMethod.PUT, "/leave-applications/*/reject-cancellation").hasAuthority(RbacPermissions.LEAVE_APPLICATION_APPROVE)
                .requestMatchers(HttpMethod.PUT, "/leave-applications/**").hasAuthority(RbacPermissions.LEAVE_APPLICATION_WRITE)
                .requestMatchers(HttpMethod.DELETE, "/leave-applications/**").hasAuthority(RbacPermissions.LEAVE_APPLICATION_WRITE)
                .anyRequest().authenticated()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterAt(new TenantAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            );

        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            ExistingUserOnlyOAuth2UserService existingUserOnlyOAuth2UserService = new ExistingUserOnlyOAuth2UserService(appUserRepository);
            http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(existingUserOnlyOAuth2UserService))
                .successHandler((request, response, authentication) -> {
                    boolean linkingFlow = OAuthLinkingContext.consumeLinkFlow(request.getSession(false));
                    String destination = linkingFlow ? "/account/security?oauthLinked=true" : "/";
                    response.sendRedirect(normalizedPublicAppUrl + destination);
                })
                .failureHandler((request, response, exception) -> {
                    String errorCode = exception instanceof OAuth2AuthenticationException oauthException
                        ? oauthException.getError().getErrorCode()
                        : "oauth_failed";
                    String encodedErrorCode = URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
                    boolean linkingFlow = OAuthLinkingContext.consumeLinkFlow(request.getSession(false));
                    String destination = linkingFlow
                        ? "/account/security?oauthError=" + encodedErrorCode
                        : "/login?oauthError=" + encodedErrorCode;
                    response.sendRedirect(normalizedPublicAppUrl + destination);
                })
            );
        }

        return http.build();
    }

    @Bean
    public TenantAuthenticationProvider tenantAuthenticationProvider(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        return new TenantAuthenticationProvider(appUserRepository, passwordEncoder);
    }

    @Bean
    public AuthenticationManager authenticationManager(TenantAuthenticationProvider tenantAuthenticationProvider) {
        return new ProviderManager(tenantAuthenticationProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String configuredOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins(configuredOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION, "X-XSRF-TOKEN", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static List<String> parseAllowedOrigins(String configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .distinct()
            .toList();
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
