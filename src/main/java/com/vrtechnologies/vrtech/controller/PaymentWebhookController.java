package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/webhooks")
public class PaymentWebhookController {

    private final OrderService orderService;

    public PaymentWebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/razorpay")
    public ApiResponse<Object> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
            HttpServletRequest request
    ) {
        orderService.handleRazorpayWebhook(payload, signature, eventId, request.getHeader("User-Agent"));
        return ApiResponse.ok("Webhook processed", null);
    }
}
