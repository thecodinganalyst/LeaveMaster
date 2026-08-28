package com.practical.leavemaster.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private static final String PENDING_PASSWORD_SENTINEL = "PENDING_ACTIVATION";

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findUniqueByLoginName(username)
                .map(appUser -> User.withUsername(appUser.getUserId())
                        .password(appUser.getPassword() == null ? PENDING_PASSWORD_SENTINEL : appUser.getPassword())
                        .disabled(!appUser.isActive() || appUser.getPassword() == null)
                        .authorities(resolveAuthorities(appUser))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found or login name is ambiguous"));
    }

    private Set<GrantedAuthority> resolveAuthorities(AppUser appUser) {
        return appUser.getRoles().stream()
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
