package com.practical.leavemaster.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "location")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(nullable = false)
    private String country;

    @Column
    private String state;
}
