package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardStoreSalesResponse {

    private Long storeId;
    private String storeName;
    private long ordersCount;
    private BigDecimal revenue;
    private long productsCount;
    private boolean active;
}
