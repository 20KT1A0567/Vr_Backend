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
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, unique = true)
    private String gatewayEventId;

    @Column(nullable = false, length = 64)
    private String gateway = "RAZORPAY";

    @Column(length = 80)
    private String eventType;

    @Column(length = 32)
    private String status = "RECEIVED";

    @Column(length = 128)
    private String gatewayOrderId;

    @Column(length = 128)
    private String gatewayPaymentId;

    @Column(length = 255)
    private String userAgent;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime processedAt;
}
