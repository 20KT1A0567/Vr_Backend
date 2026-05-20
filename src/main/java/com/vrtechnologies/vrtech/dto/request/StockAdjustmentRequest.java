package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.StockMovementType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentRequest {
    @NotNull
    private Long productId;
    private Long storeId;
    @NotNull
    private StockMovementType movementType;
    @NotNull
    private Integer quantity;
    private String reason;
}
