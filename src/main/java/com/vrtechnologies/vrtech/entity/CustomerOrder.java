package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.DeliveryType;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentMethod;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class CustomerOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean guestCheckout = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(length = 40, unique = true)
    private String orderNumber;

    @Column(length = 40, unique = true)
    private String invoiceNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(length = 80)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType = DeliveryType.PICKUP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(length = 80)
    private String deliveryState;

    @Column(name = "delivery_postal_code", length = 6)
    private String deliveryPostalCode;

    @Column(name = "promised_min_delivery_days")
    private Integer promisedMinDeliveryDays;

    @Column(name = "promised_max_delivery_days")
    private Integer promisedMaxDeliveryDays;

    @Column(name = "delivery_rule_id")
    private Long deliveryRuleId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime paidAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime cancelledAt;

    @Column(name = "courier_name", length = 80)
    private String courierName;

    @Column(name = "tracking_number", length = 80)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    private LocalDateTime returnRequestedAt;

    @Column(columnDefinition = "TEXT")
    private String returnReason;

    @Column(columnDefinition = "TEXT")
    private String returnResolutionNote;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<OrderItem> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC, id DESC")
    private Set<PaymentTransaction> paymentTransactions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private Set<OrderTimelineEvent> timelineEvents = new LinkedHashSet<>();
}
