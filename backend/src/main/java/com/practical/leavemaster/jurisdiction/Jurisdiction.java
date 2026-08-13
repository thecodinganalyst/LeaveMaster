package com.practical.leavemaster.jurisdiction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jurisdiction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jurisdiction {
    @Id
    @Column(nullable = false, unique = true, length = 32)
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction_type", nullable = false, length = 32)
    private JurisdictionType jurisdictionType;

    @Column(name = "parent_id", length = 32)
    private String parentId;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "subdivision_code", length = 32)
    private String subdivisionCode;

    @Column(nullable = false)
    private boolean active;
}
