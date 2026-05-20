package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminWishlistItemResponse {

    private Long id;
    private LocalDateTime addedAt;
    private UserSummaryResponse user;
    private ProductResponse product;
}
