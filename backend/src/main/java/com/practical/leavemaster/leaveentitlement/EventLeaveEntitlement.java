package com.practical.leavemaster.leaveentitlement;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "event_leave_entitlement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLeaveEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private String staffId;

    @Column(name = "leave_type_id", nullable = false)
    private String leaveTypeId;

    @Column(name = "policy_id", nullable = false)
    private String policyId;

    @Column(name = "qualifying_event_id", nullable = false)
    private String qualifyingEventId;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "granted_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal grantedAmount;

    @Builder.Default
    @Column(name = "used_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventLeaveEntitlementStatus status;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
