package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductReviewResponse {

    private Long id;
    private Long productId;
    private String productTitle;
    private String productImageUrl;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private Integer rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private boolean featured;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
