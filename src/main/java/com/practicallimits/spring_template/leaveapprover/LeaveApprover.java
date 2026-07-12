package com.practicallimits.spring_template.leaveapprover;

import com.practicallimits.spring_template.staff.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "leave_approver")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApprover {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "approver_id", nullable = false)
    private Staff approver;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Staff admin;

    @Column(name = "admin_date", nullable = false)
    private LocalDate adminDate;
}
