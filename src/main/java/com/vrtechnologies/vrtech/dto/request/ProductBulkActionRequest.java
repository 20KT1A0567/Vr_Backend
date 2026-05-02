package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.ProductBulkActionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductBulkActionRequest {

    @NotEmpty
    private List<Long> productIds;

    @NotNull
    private ProductBulkActionType action;

    private Boolean visible;
    private Boolean enabled;
    private Long categoryId;
    private BigDecimal priceAdjustmentPercent;
}
