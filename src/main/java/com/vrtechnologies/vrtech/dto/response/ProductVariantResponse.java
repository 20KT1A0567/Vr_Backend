package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private boolean available;
    private Map<String, String> attributes;
}
