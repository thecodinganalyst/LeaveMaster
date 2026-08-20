package com.practical.leavemaster.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_jurisdiction",
        uniqueConstraints = @UniqueConstraint(name = "UK_tenant_jurisdiction", columnNames = {"tenant_id", "jurisdiction_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantJurisdiction {

    @Id
    @Column(nullable = false, length = 255)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "jurisdiction_id", nullable = false, length = 32)
    private String jurisdictionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static String idFor(String tenantId, String jurisdictionId) {
        return tenantId + ":" + jurisdictionId;
    }
}
