package com.practical.leavemaster.leaveentitlement;

import com.practical.leavemaster.leavetype.LeaveType;
import com.practical.leavemaster.staff.Staff;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_entitlement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate from;

    @Column(name = "to_date", nullable = false)
    private LocalDate to;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal entitlement;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "policy_id")
    private String policyId;

    @Column(name = "base_entitlement_amount", precision = 10, scale = 2)
    private BigDecimal baseEntitlementAmount;

    @Builder.Default
    @Column(name = "carried_forward_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal carriedForwardAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "adjustment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(name = "generated_at")
    private Instant generatedAt;
}
