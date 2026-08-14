package com.practical.leavemaster.leaveentitlementpolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "leave_entitlement_policy_eligibility")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveEntitlementPolicyEligibilityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "policy_id", nullable = false, length = 255)
    private String policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_type", nullable = false, length = 64)
    private EligibilityCriterionType criterionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 64)
    private EligibilityOperator operator;

    @Column(name = "criterion_value", nullable = false, length = 1024)
    private String value;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
