package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardLowStockResponse {

    private Long productId;
    private String title;
    private Integer stockQuantity;
    private boolean available;
    private List<String> storeNames;
}
