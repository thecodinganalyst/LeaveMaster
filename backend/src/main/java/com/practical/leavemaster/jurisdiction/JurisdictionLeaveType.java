package com.practical.leavemaster.jurisdiction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "jurisdiction_leave_type")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JurisdictionLeaveType {
    @Id
    @Column(nullable = false, unique = true, length = 128)
    private String id;

    @Column(name = "jurisdiction_id", nullable = false, length = 32)
    private String jurisdictionId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean statutory;

    private Boolean paid;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
