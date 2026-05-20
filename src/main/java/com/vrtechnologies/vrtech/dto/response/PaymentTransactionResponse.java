package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.PaymentGateway;
import com.vrtechnologies.vrtech.entity.enums.PaymentTransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentTransactionResponse {

    private Long id;
    private PaymentGateway gateway;
    private PaymentTransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String receipt;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String gatewayStatus;
    private String failureReason;
    private String refundId;
    private BigDecimal refundedAmount;
    private String refundReason;
    private String refundStatus;
    private LocalDateTime verifiedAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
