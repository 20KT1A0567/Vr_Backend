package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.OrderRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
