package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webauthn_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Base64URL-encoded credential ID (from the authenticator) */
    @Column(name = "credential_id", nullable = false, columnDefinition = "TEXT")
    private String credentialId;

    /** COSE-encoded public key, stored as Base64URL */
    @Column(name = "public_key_cose", nullable = false, columnDefinition = "TEXT")
    private String publicKeyCose;

    /** Monotonically increasing counter to detect cloned authenticators */
    @Column(name = "sign_count", nullable = false)
    private long signCount;

    /** Human-friendly name, e.g. "MacBook Touch ID" */
    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
