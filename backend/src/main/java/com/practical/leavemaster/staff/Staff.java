package com.practical.leavemaster.staff;

import com.practical.leavemaster.leaveentitlement.LeaveEntitlement;
import com.practical.leavemaster.location.Location;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Column
    private String email;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "work_schedule_day", joinColumns = @JoinColumn(name = "staff_id"))
    private List<WorkScheduleDay> workSchedule = new ArrayList<>();

    @Column(name = "term_date")
    private LocalDate termDate;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "jurisdiction_id", length = 32)
    private String jurisdictionId;

    @Builder.Default
    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<LeaveEntitlement> leaveEntitlements = new ArrayList<>();

    @Transient
    private String loginName;

    @Column(name = "tenant_id")
    private String tenantId;
}
