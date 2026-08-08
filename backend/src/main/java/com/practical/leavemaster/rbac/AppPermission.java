package com.practical.leavemaster.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_permission")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPermission {

    @Id
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String description;
}
