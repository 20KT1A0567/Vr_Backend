package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockTransferRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Long fromStoreId;

    @NotNull
    private Long toStoreId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String reason;
}
