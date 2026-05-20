package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReturnRequestResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String reason;
    private ReturnRequestStatus status;
    private String adminNote;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private LocalDateTime pickupScheduledAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime inspectedAt;
    private String pickupAgent;
    private String pickupTrackingNumber;
    private String inspectionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
