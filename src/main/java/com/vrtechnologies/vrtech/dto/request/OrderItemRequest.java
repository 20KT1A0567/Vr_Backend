package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull
    private Long productId;

    private Long productVariantId;

    @Min(1)
    private int quantity = 1;
}
