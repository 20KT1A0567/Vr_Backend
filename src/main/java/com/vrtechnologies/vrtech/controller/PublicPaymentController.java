package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.RazorpaySettingsResponse;
import com.vrtechnologies.vrtech.service.RazorpayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicPaymentController {

    private final RazorpayService razorpayService;

    public PublicPaymentController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @GetMapping("/api/public/payments/razorpay")
    public ApiResponse<RazorpaySettingsResponse> razorpaySettings() {
        return ApiResponse.ok("Razorpay settings fetched", razorpayService.settings());
    }
}
