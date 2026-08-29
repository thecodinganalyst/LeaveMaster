package com.practical.leavemaster.contact;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_enquiry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactEnquiry {
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
    @Column(name = "enquiry_type", nullable = false, length = 40)
    private String enquiryType;
    @Column(nullable = false, length = 4000)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactEnquiryStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "first_read_at")
    private Instant firstReadAt;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ContactEnquiryReply> replies = new ArrayList<>();
}
