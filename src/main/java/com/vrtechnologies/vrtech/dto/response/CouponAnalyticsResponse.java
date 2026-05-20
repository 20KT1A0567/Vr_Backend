package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CouponAnalyticsResponse {
    private Long couponId;
    private String code;
    private int usageCount;
    private long orderCount;
    private BigDecimal discountGiven;
    private BigDecimal revenueAfterDiscount;
}
