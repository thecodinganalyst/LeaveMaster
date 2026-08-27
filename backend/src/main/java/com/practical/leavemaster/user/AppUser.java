package com.practical.leavemaster.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import com.practical.leavemaster.rbac.AppRole;

@Entity
@Table(name = "app_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @Column(name = "login_name", nullable = false)
    private String loginName;

    @Column
    private String password;

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
            joinColumns = @JoinColumn(name = "login_name"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();
}
