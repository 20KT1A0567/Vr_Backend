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
        name = "admin_backup_codes",
        indexes = {
                @Index(name = "idx_backup_code_user", columnList = "user_id")
        }
)
public class AdminBackupCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code_hash", nullable = false, length = 191)
    private String codeHash;

    @Column(name = "label", length = 16)
    private String label;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "consumed_ip", length = 64)
    private String consumedIp;
}
