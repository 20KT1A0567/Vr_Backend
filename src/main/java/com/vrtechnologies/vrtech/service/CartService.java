package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.CartItemRequest;
import com.vrtechnologies.vrtech.dto.request.UpdateQuantityRequest;
import com.vrtechnologies.vrtech.dto.response.CartItemResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserContextService userContextService;
    private final ProductService productService;

    public CartService(
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserContextService userContextService,
            ProductService productService
    ) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userContextService = userContextService;
        this.productService = productService;
    }

    public List<CartItemResponse> getCart() {
        User user = userContextService.getCurrentUser();
        return cartItemRepository.findByUserId(user.getId()).stream().map(this::toResponse).toList();
    }

    public List<CartItemResponse> addToCart(CartItemRequest request) {
        User user = userContextService.getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        validateProductAvailability(product);
        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElseGet(CartItem::new);
        int nextQuantity = (item.getQuantity() == null ? 0 : item.getQuantity()) + request.getQuantity();
        validateQuantity(product, nextQuantity);
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(nextQuantity);
        cartItemRepository.save(item);
        return getCart();
    }

    public List<CartItemResponse> updateItem(Long itemId, UpdateQuantityRequest request) {
        User user = userContextService.getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .filter(cartItem -> cartItem.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        validateProductAvailability(item.getProduct());
        validateQuantity(item.getProduct(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return getCart();
    }

    public List<CartItemResponse> removeItem(Long itemId) {
        User user = userContextService.getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .filter(cartItem -> cartItem.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
        return getCart();
    }

    public void clearCart() {
        User user = userContextService.getCurrentUser();
        cartItemRepository.deleteByUserId(user.getId());
    }

    private CartItemResponse toResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .product(productService.toProductResponse(item.getProduct()))
                .build();
    }

    private void validateProductAvailability(Product product) {
        if (!product.isAvailable() || product.getStockQuantity() == null || product.getStockQuantity() < 1) {
            throw new BadRequestException("This product is currently out of stock");
        }
    }

    private void validateQuantity(Product product, int quantity) {
        if (product.getStockQuantity() != null && quantity > product.getStockQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }
    }
}
