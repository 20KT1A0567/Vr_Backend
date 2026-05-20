package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCartItemResponse {

    private Long id;
    private Integer quantity;
    private java.math.BigDecimal estimatedValue;
    private boolean recoverable;
    private UserSummaryResponse user;
    private ProductResponse product;
}
