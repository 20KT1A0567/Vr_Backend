package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "return_requests")
public class ReturnRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnRequestStatus status = ReturnRequestStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    private LocalDateTime resolvedAt;

    private Long resolvedBy;

    private LocalDateTime pickupScheduledAt;

    private LocalDateTime pickedUpAt;

    private LocalDateTime inspectedAt;

    @Column(length = 120)
    private String pickupAgent;

    @Column(length = 80)
    private String pickupTrackingNumber;

    @Column(columnDefinition = "TEXT")
    private String inspectionNote;
}
