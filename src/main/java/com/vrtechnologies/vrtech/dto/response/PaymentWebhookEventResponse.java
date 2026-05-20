package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentWebhookEventResponse {
    private Long id;
    private String gatewayEventId;
    private String gateway;
    private String eventType;
    private String status;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String errorMessage;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
