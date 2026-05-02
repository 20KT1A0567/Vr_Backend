package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.config.JsonMapConverter;
import com.vrtechnologies.vrtech.entity.enums.PaymentGateway;
import com.vrtechnologies.vrtech.entity.enums.PaymentTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentGateway gateway = PaymentGateway.RAZORPAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentTransactionStatus status = PaymentTransactionStatus.CREATED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    private String currency = "INR";

    @Column(length = 64)
    private String receipt;

    @Column(length = 64)
    private String gatewayOrderId;

    @Column(length = 64)
    private String gatewayPaymentId;

    @Column(length = 128)
    private String gatewaySignature;

    @Column(length = 64)
    private String gatewayEventId;

    @Column(length = 64)
    private String gatewayStatus;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    private LocalDateTime verifiedAt;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;
}
