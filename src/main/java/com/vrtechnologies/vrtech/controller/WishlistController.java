package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.service.WishlistService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getWishlist() {
        return ApiResponse.ok("Wishlist fetched", wishlistService.getWishlist());
    }

    @PostMapping("/{productId}")
    public ApiResponse<List<ProductResponse>> addToWishlist(@PathVariable Long productId) {
        return ApiResponse.ok("Wishlist updated", wishlistService.addToWishlist(productId));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<List<ProductResponse>> removeFromWishlist(@PathVariable Long productId) {
        return ApiResponse.ok("Wishlist updated", wishlistService.removeFromWishlist(productId));
    }
}
