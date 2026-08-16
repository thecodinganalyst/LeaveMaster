package com.practical.leavemaster.leaveentitlementpolicy;

import com.practical.leavemaster.config.ConfigurationScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_entitlement_policy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveEntitlementPolicy {
    @Id
    private String id;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ConfigurationScope scope = ConfigurationScope.TENANT;

    @Column(name = "jurisdiction_id", length = 32)
    private String jurisdictionId;

    @Column(name = "leave_type_id", length = 255)
    private String leaveTypeId;

    @Column(name = "jurisdiction_leave_type_id", length = 128)
    private String jurisdictionLeaveTypeId;

    @Column(name = "source_template_id", length = 255)
    private String sourceTemplateId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_unit", nullable = false, length = 32)
    private EntitlementUnit entitlementUnit;

    @Column(name = "entitlement_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal entitlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "accrual_method", nullable = false, length = 32)
    private AccrualMethod accrualMethod;

    @Column(name = "accrual_rate", precision = 12, scale = 4)
    private BigDecimal accrualRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "proration_method", nullable = false, length = 32)
    private ProrationMethod prorationMethod;

    @Column(name = "carry_forward_allowed", nullable = false)
    private boolean carryForwardAllowed;

    @Column(name = "carry_forward_limit", precision = 12, scale = 4)
    private BigDecimal carryForwardLimit;

    @Column(name = "carry_forward_expiry_months")
    private Integer carryForwardExpiryMonths;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void ensureId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}
