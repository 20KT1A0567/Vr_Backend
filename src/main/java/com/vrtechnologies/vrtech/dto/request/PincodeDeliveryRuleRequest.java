package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PincodeDeliveryRuleRequest {

    @Pattern(regexp = "\\d{6}", message = "Pincode must be exactly 6 digits")
    private String pincode;

    private String countryCode;
    private String stateName;
    private String cityName;
    private String zoneName;
    private Boolean serviceable;
    private Boolean codAvailable;
    private Boolean prepaidAvailable;
    private BigDecimal deliveryCharge;
    private BigDecimal freeDeliveryThreshold;
    private Integer minDeliveryDays;
    private Integer maxDeliveryDays;
    private Long storeId;
    private Integer priority;
    private Boolean active;
    private String notes;
}
