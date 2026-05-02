package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.PaymentGateway;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentCheckoutSessionResponse {

    private Long orderId;
    private String orderNumber;
    private Long transactionId;
    private PaymentGateway gateway;
    private String keyId;
    private String gatewayOrderId;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String description;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
