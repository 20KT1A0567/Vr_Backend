package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private Long id;
    private Integer quantity;
    private BigDecimal priceAtTime;
    private ProductResponse product;
    private ProductVariantResponse variant;
}
