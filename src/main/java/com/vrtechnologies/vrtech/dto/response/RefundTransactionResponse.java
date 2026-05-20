package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RefundTransactionResponse {
    private Long id;
    private Long orderId;
    private Long paymentTransactionId;
    private String refundId;
    private BigDecimal amount;
    private String status;
    private String reason;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
