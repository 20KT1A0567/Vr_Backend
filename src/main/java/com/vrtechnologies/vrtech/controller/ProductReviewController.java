package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.ProductReviewRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.ProductReviewResponse;
import com.vrtechnologies.vrtech.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    public ProductReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @PostMapping("/api/users/reviews")
    public ApiResponse<ProductReviewResponse> submit(@Valid @RequestBody ProductReviewRequest request) {
        return ApiResponse.ok("Review submitted for moderation", productReviewService.submitCustomerReview(request));
    }
}
