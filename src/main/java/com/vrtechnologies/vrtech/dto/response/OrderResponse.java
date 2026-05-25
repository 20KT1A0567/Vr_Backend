package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.DeliveryType;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentMethod;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String invoiceNumber;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal deliveryCharge;
    private String couponCode;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private DeliveryType deliveryType;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private StoreSummaryResponse store;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String deliveryAddress;
    private String deliveryState;
    private String deliveryPostalCode;
    private Integer promisedMinDeliveryDays;
    private Integer promisedMaxDeliveryDays;
    private String notes;
    private String cancellationReason;
    private String returnReason;
    private String returnResolutionNote;
    private LocalDateTime paidAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime returnRequestedAt;
    private LocalDateTime shippedAt;
    private String courierName;
    private String trackingNumber;
    private String trackingUrl;
    private PaymentTransactionResponse latestPayment;
    private List<OrderTimelineEventResponse> timeline;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
