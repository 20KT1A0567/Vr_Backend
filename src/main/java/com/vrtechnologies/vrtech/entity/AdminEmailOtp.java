package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "admin_email_otps",
        indexes = {
                @Index(name = "idx_admin_email_otp_user", columnList = "user_id"),
                @Index(name = "idx_admin_email_otp_challenge", columnList = "challenge_id", unique = true)
        }
)
public class AdminEmailOtp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false, length = 64)
    private String challengeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email", nullable = false, length = 191)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 191)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
