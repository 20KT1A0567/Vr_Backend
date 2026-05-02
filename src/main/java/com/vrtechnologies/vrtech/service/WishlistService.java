package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.WishlistItem;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserContextService userContextService;
    private final ProductService productService;

    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            UserContextService userContextService,
            ProductService productService
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userContextService = userContextService;
        this.productService = productService;
    }

    public List<ProductResponse> getWishlist() {
        User user = userContextService.getCurrentUser();
        return wishlistItemRepository.findByUserIdOrderByAddedAtDesc(user.getId())
                .stream()
                .map(WishlistItem::getProduct)
                .map(productService::toProductResponse)
                .toList();
    }

    public List<ProductResponse> addToWishlist(Long productId) {
        User user = userContextService.getCurrentUser();
        wishlistItemRepository.findByUserIdAndProductId(user.getId(), productId).orElseGet(() -> {
            Product product = productRepository.findById(productId).orElseThrow();
            WishlistItem item = new WishlistItem();
            item.setUser(user);
            item.setProduct(product);
            return wishlistItemRepository.save(item);
        });
        return getWishlist();
    }

    @Transactional
    public List<ProductResponse> removeFromWishlist(Long productId) {
        User user = userContextService.getCurrentUser();
        wishlistItemRepository.deleteByUserIdAndProductId(user.getId(), productId);
        return getWishlist();
    }
}
