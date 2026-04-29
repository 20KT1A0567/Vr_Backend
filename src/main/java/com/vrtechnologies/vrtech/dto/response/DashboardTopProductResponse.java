package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardTopProductResponse {

    private Long productId;
    private String title;
    private long soldQuantity;
    private BigDecimal revenue;
    private Integer stockQuantity;
    private List<String> storeNames;
}
