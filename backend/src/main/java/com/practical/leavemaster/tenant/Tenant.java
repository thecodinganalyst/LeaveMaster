package com.practical.leavemaster.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "jurisdiction_id", nullable = false, length = 32)
    private String jurisdictionId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Transient
    @Builder.Default
    private List<TenantJurisdictionProvisionRequest> jurisdictions = new ArrayList<>();

    @Transient
    private LocalDate calendarStart;

    @Transient
    private LocalDate calendarEnd;

    @PrePersist
    @PreUpdate
    void refreshLastModified() {
        lastModified = LocalDateTime.now();
    }
}
