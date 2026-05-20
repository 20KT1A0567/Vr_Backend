package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StockTransferResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private Long fromStoreId;
    private String fromStoreName;
    private Long toStoreId;
    private String toStoreName;
    private Integer quantity;
    private String reason;
    private Long initiatedById;
    private String initiatedByEmail;
    private Long outMovementId;
    private Long inMovementId;
    private LocalDateTime createdAt;
}
