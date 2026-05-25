package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PincodeDeliveryCheckResponse {
    private String pincode;
    private boolean serviceable;
    private boolean deliveryEnabled;
    private boolean codAvailable;
    private boolean prepaidAvailable;
    private BigDecimal deliveryCharge;
    private BigDecimal freeDeliveryThreshold;
    private boolean freeDeliveryApplied;
    private Integer minDeliveryDays;
    private Integer maxDeliveryDays;
    private String estimatedLabel;
    private String expectedMinDate;
    private String expectedMaxDate;
    private Long storeId;
    private String storeName;
    private Long ruleId;
    private String ruleSource;
    private String message;
}
