package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BackInStockRequestResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
