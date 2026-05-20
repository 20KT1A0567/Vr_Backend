package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRecoveryResponse {
    private Long orderId;
    private String orderNumber;
    private String emailStatus;
    private String whatsappStatus;
    private String message;
}
