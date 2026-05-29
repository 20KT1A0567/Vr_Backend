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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart() {
        User user = userContextService.getCurrentUser();
        return buildNormalizedCartResponse(user.getId());
    }

    @Transactional
    public List<CartItemResponse> addToCart(CartItemRequest request) {
        User user = userContextService.getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        validateProductAvailability(product);
        List<CartItem> matchingItems = cartItemRepository.findAllByUserIdAndProductIdOrderByIdAsc(user.getId(), product.getId());
        CartItem item = matchingItems.stream().findFirst().orElseGet(CartItem::new);
        int nextQuantity = matchingItems.stream().mapToInt(this::getQuantityOrZero).sum() + getQuantityOrDefault(request.getQuantity());
        validateQuantity(product, nextQuantity);
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(nextQuantity);
        item.setRecoveryNotified(false);
        cartItemRepository.save(item);
        deleteDuplicateRows(item, matchingItems);
        return buildNormalizedCartResponse(user.getId());
    }

    @Transactional
    public List<CartItemResponse> updateItem(Long itemId, UpdateQuantityRequest request) {
        User user = userContextService.getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .filter(cartItem -> cartItem.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        validateProductAvailability(item.getProduct());
        int nextQuantity = getRequiredQuantity(request.getQuantity());
        validateQuantity(item.getProduct(), nextQuantity);
        item.setQuantity(nextQuantity);
        item.setRecoveryNotified(false);
        cartItemRepository.save(item);
        deleteDuplicateRows(item, cartItemRepository.findAllByUserIdAndProductIdOrderByIdAsc(user.getId(), item.getProduct().getId()));
        return buildNormalizedCartResponse(user.getId());
    }

    @Transactional
    public List<CartItemResponse> removeItem(Long itemId) {
        User user = userContextService.getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .filter(cartItem -> cartItem.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.deleteAll(cartItemRepository.findAllByUserIdAndProductIdOrderByIdAsc(user.getId(), item.getProduct().getId()));
        return buildNormalizedCartResponse(user.getId());
    }

    @Transactional
    public void clearCart() {
        User user = userContextService.getCurrentUser();
        cartItemRepository.deleteByUserId(user.getId());
    }

    private List<CartItemResponse> buildNormalizedCartResponse(Long userId) {
        Map<Long, CartItem> canonicalItems = new LinkedHashMap<>();
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();

        for (CartItem item : cartItemRepository.findByUserIdOrderByIdAsc(userId)) {
            Long productId = item.getProduct().getId();
            canonicalItems.putIfAbsent(productId, item);
            quantityByProductId.merge(productId, getQuantityOrZero(item), Integer::sum);
        }

        List<CartItemResponse> responses = new ArrayList<>();
        for (Map.Entry<Long, CartItem> entry : canonicalItems.entrySet()) {
            responses.add(toResponse(entry.getValue(), quantityByProductId.get(entry.getKey())));
        }
        return responses;
    }

    private CartItemResponse toResponse(CartItem item) {
        return toResponse(item, item.getQuantity());
    }

    private CartItemResponse toResponse(CartItem item, Integer quantity) {
        return CartItemResponse.builder()
                .id(item.getId())
                .quantity(quantity)
                .product(productService.toProductResponse(item.getProduct()))
                .build();
    }

    private void deleteDuplicateRows(CartItem primaryItem, List<CartItem> matchingItems) {
        if (matchingItems.isEmpty()) {
            return;
        }

        List<CartItem> duplicates = matchingItems.stream()
                .filter(existingItem -> !existingItem.getId().equals(primaryItem.getId()))
                .toList();
        if (!duplicates.isEmpty()) {
            cartItemRepository.deleteAll(duplicates);
        }
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

    private int getQuantityOrZero(CartItem item) {
        return item.getQuantity() == null ? 0 : item.getQuantity();
    }

    private int getQuantityOrDefault(Integer quantity) {
        return quantity == null ? 1 : quantity;
    }

    private int getRequiredQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BadRequestException("Quantity is required");
        }
        return quantity;
    }
}
