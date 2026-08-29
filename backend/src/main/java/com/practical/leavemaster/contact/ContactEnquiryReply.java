package com.practical.leavemaster.contact;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customer_enquiry_reply")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactEnquiryReply {
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private ContactEnquiry enquiry;

    @Column(name = "reply_body", nullable = false, length = 4000)
    private String replyBody;
    @Column(name = "replied_by", nullable = false, length = 120)
    private String repliedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
