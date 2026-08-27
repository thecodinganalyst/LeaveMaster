package com.practical.leavemaster.accountactivation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_activation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivation {

    @Id
    @Column(name = "login_name", nullable = false)
    private String loginName;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "request_window_started_at", nullable = false)
    private LocalDateTime requestWindowStartedAt;

    @Column(name = "request_count", nullable = false)
    private int requestCount;
}
