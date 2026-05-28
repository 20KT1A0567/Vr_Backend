package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.OrderActionRequest;
import com.vrtechnologies.vrtech.dto.request.OrderRequest;
import com.vrtechnologies.vrtech.dto.request.PaymentVerificationRequest;
import com.vrtechnologies.vrtech.dto.request.CourierWebhookRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.CheckoutProfileResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.OrderTimelineEventResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentCheckoutSessionResponse;
import com.vrtechnologies.vrtech.service.OrderService;
import com.vrtechnologies.vrtech.service.ReturnRequestService;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;

    public OrderController(OrderService orderService, ReturnRequestService returnRequestService) {
        this.orderService = orderService;
        this.returnRequestService = returnRequestService;
    }

    @PostMapping("/api/orders/place")
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.placeOrder(request);
        String message = request.getPaymentMethod() != null && !"CASH".equals(request.getPaymentMethod().name())
                ? "Payment pending"
                : "Order placed";
        return ApiResponse.ok(message, order);
    }

    @PostMapping("/api/orders/guest")
    public ApiResponse<OrderResponse> placeGuestOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.placeGuestOrder(request);
        String message = request.getPaymentMethod() != null && !"CASH".equals(request.getPaymentMethod().name())
                ? "Payment pending"
                : "Guest order placed";
        return ApiResponse.ok(message, order);
    }

    @PostMapping("/api/courier/webhooks/status")
    public ApiResponse<OrderResponse> courierWebhook(@RequestBody CourierWebhookRequest request) {
        return ApiResponse.ok("Courier status processed", orderService.handleCourierWebhook(request));
    }

    @GetMapping("/api/users/orders")
    public ApiResponse<List<OrderResponse>> getMyOrders() {
        return ApiResponse.ok("Orders fetched", orderService.getMyOrders());
    }

    @GetMapping("/api/users/checkout-profile")
    public ApiResponse<CheckoutProfileResponse> getCheckoutProfile() {
        return ApiResponse.ok("Checkout profile fetched", orderService.getCheckoutProfile());
    }

    @GetMapping("/api/users/orders/{id}")
    public ApiResponse<OrderResponse> getMyOrder(@PathVariable Long id) {
        return ApiResponse.ok("Order fetched", orderService.getMyOrder(id));
    }

    @GetMapping("/api/public/orders/track")
    public ApiResponse<OrderResponse> trackOrder(@RequestParam String orderNumber, @RequestParam String phone) {
        return ApiResponse.ok("Order tracked", orderService.trackPublicOrder(orderNumber, phone));
    }

    @GetMapping("/api/users/orders/{id}/timeline")
    public ApiResponse<List<OrderTimelineEventResponse>> getMyOrderTimeline(@PathVariable Long id) {
        return ApiResponse.ok("Order timeline fetched", orderService.getMyOrder(id).getTimeline());
    }

    @PostMapping("/api/users/orders/{id}/payment-order")
    public ApiResponse<PaymentCheckoutSessionResponse> createPaymentOrder(@PathVariable Long id) {
        return ApiResponse.ok("Payment session created", orderService.createPaymentCheckout(id));
    }

    @PostMapping("/api/users/orders/{id}/verify-payment")
    public ApiResponse<OrderResponse> verifyPayment(@PathVariable Long id, @Valid @RequestBody PaymentVerificationRequest request) {
        return ApiResponse.ok("Payment verified", orderService.verifyPayment(id, request));
    }

    @PostMapping("/api/users/orders/{id}/payment-failed")
    public ApiResponse<OrderResponse> markPaymentFailed(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        String reason = request == null ? null : request.get("reason");
        return ApiResponse.ok("Payment marked failed", orderService.markPaymentFailed(id, reason));
    }

    @PatchMapping("/api/users/orders/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.ok("Order cancelled", orderService.cancelMyOrder(id, request.getReason()));
    }

    @PatchMapping("/api/users/orders/{id}/return-request")
    public ApiResponse<OrderResponse> requestReturn(@PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        return ApiResponse.ok("Return requested", orderService.getMyOrder(returnRequestService.requestReturn(id, request.getReason()).getOrderId()));
    }

    @GetMapping(value = "/api/users/orders/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, @RequestParam(required = false) String format) {
        OrderResponse order = orderService.getMyOrder(id);
        if ("WORD".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + order.getInvoiceNumber() + ".doc\"")
                    .contentType(MediaType.parseMediaType("application/msword"))
                    .body(orderService.generateInvoiceWordForUser(id));
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + order.getInvoiceNumber() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(orderService.generateInvoicePdfForUser(id));
        }
    }
}
