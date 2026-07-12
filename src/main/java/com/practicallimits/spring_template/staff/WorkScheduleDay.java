package com.practicallimits.spring_template.staff;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleDay {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_schedule", nullable = false)
    private DaySchedule daySchedule;
}
