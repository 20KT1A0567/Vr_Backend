package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "admin_activity_logs",
        indexes = {
                @Index(name = "idx_activity_admin", columnList = "admin_id"),
                @Index(name = "idx_activity_module", columnList = "module"),
                @Index(name = "idx_activity_created", columnList = "created_at")
        }
)
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_email", length = 191)
    private String adminEmail;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PermissionAction action;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(length = 1000)
    private String description;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "current_hash", length = 64)
    private String currentHash = "";

    @Column(name = "previous_hash", length = 64)
    private String previousHash = "";
}
