package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.CartItemRequest;
import com.vrtechnologies.vrtech.dto.request.UpdateQuantityRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.CartItemResponse;
import com.vrtechnologies.vrtech.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartItemResponse>> getCart() {
        return ApiResponse.ok("Cart fetched", cartService.getCart());
    }

    @PostMapping("/add")
    public ApiResponse<List<CartItemResponse>> addToCart(@Valid @RequestBody CartItemRequest request) {
        return ApiResponse.ok("Cart updated", cartService.addToCart(request));
    }

    @PutMapping("/update/{itemId}")
    public ApiResponse<List<CartItemResponse>> updateCartItem(@PathVariable Long itemId, @Valid @RequestBody UpdateQuantityRequest request) {
        return ApiResponse.ok("Cart updated", cartService.updateItem(itemId, request));
    }

    @DeleteMapping("/remove/{itemId}")
    public ApiResponse<List<CartItemResponse>> removeItem(@PathVariable Long itemId) {
        return ApiResponse.ok("Cart updated", cartService.removeItem(itemId));
    }

    @DeleteMapping("/clear")
    public ApiResponse<Object> clearCart() {
        cartService.clearCart();
        return ApiResponse.ok("Cart cleared", null);
    }
}
