package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PincodeDeliveryRuleResponse {
    private Long id;
    private String pincode;
    private String countryCode;
    private String stateName;
    private String cityName;
    private String zoneName;
    private boolean serviceable;
    private boolean codAvailable;
    private boolean prepaidAvailable;
    private BigDecimal deliveryCharge;
    private BigDecimal freeDeliveryThreshold;
    private Integer minDeliveryDays;
    private Integer maxDeliveryDays;
    private Long storeId;
    private String storeName;
    private Integer priority;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
