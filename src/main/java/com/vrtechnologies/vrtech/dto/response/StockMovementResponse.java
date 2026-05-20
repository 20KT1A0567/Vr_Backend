package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.StockMovementType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StockMovementResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private Long storeId;
    private String storeName;
    private StockMovementType movementType;
    private Integer quantity;
    private Integer previousStock;
    private Integer newStock;
    private String reason;
    private String actorEmail;
    private LocalDateTime createdAt;
}
