package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateProductVariantRequest {
    private String sku;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean available;
    private List<Long> attributeValueIds;
}
