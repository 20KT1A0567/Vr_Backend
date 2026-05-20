package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourierWebhookRequest {
    private String orderNumber;
    private String trackingNumber;
    private String courierName;
    private String status;
    private String rawStatus;
    private String deliveredAt;
}
