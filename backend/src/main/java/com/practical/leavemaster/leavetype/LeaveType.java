package com.practical.leavemaster.leavetype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "leave_type")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean statutory;

    private Boolean paid;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "source_jurisdiction_leave_type_id", length = 128)
    private String sourceJurisdictionLeaveTypeId;

    /**
     * Tenant-safe derived value used by the frontend jurisdiction filter. This is resolved
     * from the platform catalogue internally so tenant users do not need direct access to
     * the platform-only jurisdiction-leave-types endpoint.
     */
    @Transient
    private String jurisdictionId;
}
