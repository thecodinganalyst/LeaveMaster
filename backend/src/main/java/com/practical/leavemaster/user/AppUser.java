package com.practical.leavemaster.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.practical.leavemaster.rbac.AppRole;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_app_user_tenant_login",
                        columnNames = {"tenant_id", "login_name"}),
                @UniqueConstraint(
                        name = "uk_app_user_oauth_identity",
                        columnNames = {"oidc_provider", "oidc_subject"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private String userId;

    @Column(name = "login_name", nullable = false)
    private String loginName;

    @Column
    private String password;

    @Column(length = 320)
    private String email;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "oidc_provider")
    private String oidcProvider;

    @Column(name = "oidc_subject")
    private String oidcSubject;

    @Column(name = "tenant_id")
    private String tenantId;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "app_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();

    @PrePersist
    void ensureUserId() {
        if (userId == null || userId.isBlank()) {
            userId = UUID.randomUUID().toString();
        }
    }
}
