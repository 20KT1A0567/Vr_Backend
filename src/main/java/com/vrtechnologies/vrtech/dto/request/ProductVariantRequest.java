package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class ProductVariantRequest {
    private String sku;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private boolean available = true;
    private Map<String, String> attributeSelections;
}
