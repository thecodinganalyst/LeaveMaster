package com.practical.leavemaster.leaveeligibility;

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
@Table(name = "staff_dependant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDependant {

    @Id
    @Column(nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "staff_id", nullable = false)
    private String staffId;

    @Column(nullable = false)
    private String name;

    @Column(name = "relationship_code", nullable = false, length = 64)
    private String relationshipCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "citizenship_code", length = 64)
    private String citizenshipCode;

    @Column(name = "residency_code", length = 64)
    private String residencyCode;

    @Column(name = "adoption_date")
    private LocalDate adoptionDate;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active;
}
