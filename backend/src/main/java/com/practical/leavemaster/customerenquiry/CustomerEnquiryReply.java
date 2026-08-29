package com.practical.leavemaster.customerenquiry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_enquiry_reply")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEnquiryReply {
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private CustomerEnquiry enquiry;

    @Column(name = "reply_body", nullable = false, length = 4000)
    private String replyBody;

    @Column(name = "replied_by", nullable = false, length = 120)
    private String repliedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
