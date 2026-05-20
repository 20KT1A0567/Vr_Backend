package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.CouponValidationRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.CouponValidationResponse;
import com.vrtechnologies.vrtech.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/api/coupons/validate")
    public ApiResponse<CouponValidationResponse> validate(@Valid @RequestBody CouponValidationRequest request) {
        return ApiResponse.ok("Coupon validated", couponService.validate(request.getCode(), request.getSubtotal()));
    }
}
