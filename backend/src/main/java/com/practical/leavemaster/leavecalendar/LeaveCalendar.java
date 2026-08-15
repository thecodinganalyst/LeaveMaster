package com.practical.leavemaster.leavecalendar;

import com.practical.leavemaster.config.ConfigurationScope;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_calendar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCalendar {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(name = "start_date", nullable = false)
    private LocalDate start;

    @Column(name = "end_date", nullable = false)
    private LocalDate end;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "public_holiday", joinColumns = @JoinColumn(name = "leave_calendar_id"))
    private List<PublicHoliday> publicHolidays = new ArrayList<>();

    @Column(name = "tenant_id")
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ConfigurationScope scope = ConfigurationScope.TENANT;

    @Column(name = "jurisdiction_id", length = 32)
    private String jurisdictionId;

    @Column(name = "source_template_id", length = 255)
    private String sourceTemplateId;
}
