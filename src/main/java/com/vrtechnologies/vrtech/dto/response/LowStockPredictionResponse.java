package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LowStockPredictionResponse {
    private Long productId;
    private String productTitle;
    private String productSku;
    private int currentStock;
    private int lowStockThreshold;
    private int leadTimeDays;
    private double velocity30d;
    private double velocity7d;
    private double velocity3d;
    private double averageDailyVelocity;
    private double spikeFactor;
    private Integer daysToStockout;
    private int recommendedReorder;
    private String warningMessage;
}
