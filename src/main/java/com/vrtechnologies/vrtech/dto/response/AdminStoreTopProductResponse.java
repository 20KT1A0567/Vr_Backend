package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminStoreTopProductResponse {

    private Long productId;
    private String title;
    private long soldQuantity;
    private BigDecimal revenue;
    private Integer stockQuantity;
    private boolean available;
}
