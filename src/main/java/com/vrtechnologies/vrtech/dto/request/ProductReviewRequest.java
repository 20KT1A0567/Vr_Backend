package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.ReviewStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductReviewRequest {

    private Long productId;

    private Long userId;

    @NotBlank
    @Size(max = 120)
    private String customerName;

    @Size(max = 160)
    private String customerEmail;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 160)
    private String title;

    @NotBlank
    private String comment;

    private ReviewStatus status = ReviewStatus.PENDING;

    private boolean featured;

    private String adminNote;

    private Boolean verifiedPurchase;
}
