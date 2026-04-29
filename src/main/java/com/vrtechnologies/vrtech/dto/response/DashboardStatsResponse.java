package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardStatsResponse {

    private long totalProducts;
    private long totalUsers;
    private long totalOrders;
    private long totalStores;
    private long activeStores;
    private BigDecimal totalRevenue;
    private long newEnquiries;
    private long pendingOrders;
    private long lowStockProducts;
    private List<DashboardOrderStatusResponse> orderStatuses;
    private List<DashboardStoreSalesResponse> storeSales;
    private List<DashboardTopProductResponse> topProducts;
    private List<DashboardLowStockResponse> lowStockItems;
    private List<DashboardRecentOrderResponse> recentOrders;
}
