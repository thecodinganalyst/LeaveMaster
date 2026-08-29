package com.practical.leavemaster.customerenquiry;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_enquiry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEnquiry {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String company;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "company_size", length = 60)
    private String companySize;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "enquiry_type", nullable = false, length = 40)
    private CustomerEnquiryType enquiryType;

    @Column(nullable = false, length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerEnquiryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "first_read_at")
    private LocalDateTime firstReadAt;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CustomerEnquiryReply> replies = new ArrayList<>();

    @PrePersist
    void initialize() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = CustomerEnquiryStatus.NEW;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
