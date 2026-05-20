package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notification_logs")
public class NotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 40)
    private String channel;

    private String recipient;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    private String status = "QUEUED";

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    private Long orderId;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 3;

    private LocalDateTime nextAttemptAt;

    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;
}
