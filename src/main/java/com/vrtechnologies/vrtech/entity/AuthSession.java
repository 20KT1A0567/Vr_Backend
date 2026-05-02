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
        name = "auth_sessions",
        indexes = {
                @Index(name = "idx_auth_session_user", columnList = "user_id"),
                @Index(name = "idx_auth_session_token_hash", columnList = "refresh_token_hash", unique = true)
        }
)
public class AuthSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 191)
    private String userEmail;

    @Column(name = "refresh_token_hash", nullable = false, length = 128, unique = true)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    public boolean isActive() {
        return revokedAt == null && logoutAt == null && refreshTokenExpiresAt != null
                && refreshTokenExpiresAt.isAfter(LocalDateTime.now());
    }
}
