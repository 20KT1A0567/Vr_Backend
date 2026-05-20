package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CouponValidationResponse {
    private String code;
    private boolean valid;
    private String message;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BigDecimal minOrder;
    private LocalDate expiryDate;
    private Integer remainingUses;
}
