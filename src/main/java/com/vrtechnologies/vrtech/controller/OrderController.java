package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.OrderActionRequest;
import com.vrtechnologies.vrtech.dto.request.OrderRequest;
import com.vrtechnologies.vrtech.dto.request.PaymentVerificationRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentCheckoutSessionResponse;
import com.vrtechnologies.vrtech.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders/place")
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.ok("Order placed", orderService.placeOrder(request));
    }

    @GetMapping("/api/users/orders")
    public ApiResponse<List<OrderResponse>> getMyOrders() {
        return ApiResponse.ok("Orders fetched", orderService.getMyOrders());
    }

    @GetMapping("/api/users/orders/{id}")
    public ApiResponse<OrderResponse> getMyOrder(@PathVariable Long id) {
        return ApiResponse.ok("Order fetched", orderService.getMyOrder(id));
    }

    @PostMapping("/api/users/orders/{id}/payment-order")
    public ApiResponse<PaymentCheckoutSessionResponse> createPaymentOrder(@PathVariable Long id) {
        return ApiResponse.ok("Payment session created", orderService.createPaymentCheckout(id));
    }

    @PostMapping("/api/users/orders/{id}/verify-payment")
    public ApiResponse<OrderResponse> verifyPayment(@PathVariable Long id, @Valid @RequestBody PaymentVerificationRequest request) {
        return ApiResponse.ok("Payment verified", orderService.verifyPayment(id, request));
    }

    @PatchMapping("/api/users/orders/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.ok("Order cancelled", orderService.cancelMyOrder(id, request.getReason()));
    }

    @PatchMapping("/api/users/orders/{id}/return-request")
    public ApiResponse<OrderResponse> requestReturn(@PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.ok("Return requested", orderService.requestReturn(id, request.getReason()));
    }

    @GetMapping(value = "/api/users/orders/{id}/invoice", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> downloadInvoice(@PathVariable Long id) {
        OrderResponse order = orderService.getMyOrder(id);
        String html = orderService.generateInvoiceHtmlForUser(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + order.getInvoiceNumber() + ".html\"")
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
