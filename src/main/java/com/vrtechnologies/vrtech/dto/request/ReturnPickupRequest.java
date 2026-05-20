package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReturnPickupRequest {
    private LocalDateTime pickupScheduledAt;
    private String pickupAgent;
    private String pickupTrackingNumber;
    private String note;
}
