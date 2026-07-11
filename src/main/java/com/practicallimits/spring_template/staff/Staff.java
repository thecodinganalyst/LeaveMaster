package com.practicallimits.spring_template.staff;

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
@Table(name = "staff")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "work_schedule", nullable = false)
    private String workSchedule;

    @Column(name = "term_date")
    private LocalDate termDate;
}
