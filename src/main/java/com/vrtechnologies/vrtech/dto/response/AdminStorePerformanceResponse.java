package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminStorePerformanceResponse {

    private Long storeId;
    private String storeName;
    private String city;
    private boolean active;
    private long productsCount;
    private long activeProductsCount;
    private long lowStockProductsCount;
    private long ordersCount;
    private long pendingOrdersCount;
    private long deliveredOrdersCount;
    private long cancelledOrdersCount;
    private long unitsSold;
    private BigDecimal revenue;
    private BigDecimal pipelineRevenue;
    private BigDecimal averageOrderValue;
    private BigDecimal revenueContributionPercent;
    private BigDecimal revenuePerActiveProduct;
    private BigDecimal unitsPerOrder;
    private BigDecimal deliveredRate;
    private BigDecimal cancellationRate;
    private BigDecimal topProductRevenueShare;
    private LocalDateTime lastOrderAt;
    private List<AdminStoreTopProductResponse> topProducts;
}
