package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.CartItemRequest;
import com.vrtechnologies.vrtech.dto.request.UpdateQuantityRequest;
import com.vrtechnologies.vrtech.dto.response.CartItemResponse;
import com.vrtechnologies.vrtech.dto.response.ProductVariantResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductVariant;
import com.vrtechnologies.vrtech.entity.AttributeValue;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserContextService userContextService;
    private final ProductService productService;
    private final ProductVariantRepository productVariantRepository;

    public CartService(
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserContextService userContextService,
            ProductService productService,
            ProductVariantRepository productVariantRepository
    ) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userContextService = userContextService;
        this.productService = productService;
        this.productVariantRepository = productVariantRepository;
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

        ProductVariant variant = null;
        if (request.getProductVariantId() != null) {
            variant = productVariantRepository.findById(request.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BadRequestException("Variant does not belong to this product");
            }
        }

        validateProductOrVariantAvailability(product, variant);

        final ProductVariant finalVariant = variant;
        List<CartItem> matchingItems = cartItemRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()) &&
                        (finalVariant == null ? item.getProductVariant() == null :
                         item.getProductVariant() != null && item.getProductVariant().getId().equals(finalVariant.getId())))
                .collect(Collectors.toList());

        CartItem item = matchingItems.stream().findFirst().orElseGet(CartItem::new);
        int nextQuantity = matchingItems.stream().mapToInt(this::getQuantityOrZero).sum() + getQuantityOrDefault(request.getQuantity());
        validateProductOrVariantQuantity(product, variant, nextQuantity);

        item.setUser(user);
        item.setProduct(product);
        item.setProductVariant(variant);
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

        validateProductOrVariantAvailability(item.getProduct(), item.getProductVariant());
        int nextQuantity = getRequiredQuantity(request.getQuantity());
        validateProductOrVariantQuantity(item.getProduct(), item.getProductVariant(), nextQuantity);

        item.setQuantity(nextQuantity);
        item.setRecoveryNotified(false);
        cartItemRepository.save(item);

        List<CartItem> matchingItems = cartItemRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .filter(existing -> existing.getProduct().getId().equals(item.getProduct().getId()) &&
                        (item.getProductVariant() == null ? existing.getProductVariant() == null :
                         existing.getProductVariant() != null && existing.getProductVariant().getId().equals(item.getProductVariant().getId())))
                .collect(Collectors.toList());

        deleteDuplicateRows(item, matchingItems);
        return buildNormalizedCartResponse(user.getId());
    }

    @Transactional
    public List<CartItemResponse> removeItem(Long itemId) {
        User user = userContextService.getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .filter(cartItem -> cartItem.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        List<CartItem> matchingItems = cartItemRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .filter(existing -> existing.getProduct().getId().equals(item.getProduct().getId()) &&
                        (item.getProductVariant() == null ? existing.getProductVariant() == null :
                         existing.getProductVariant() != null && existing.getProductVariant().getId().equals(item.getProductVariant().getId())))
                .collect(Collectors.toList());

        cartItemRepository.deleteAll(matchingItems);
        return buildNormalizedCartResponse(user.getId());
    }

    @Transactional
    public void clearCart() {
        User user = userContextService.getCurrentUser();
        cartItemRepository.deleteByUserId(user.getId());
    }

    private List<CartItemResponse> buildNormalizedCartResponse(Long userId) {
        Map<String, CartItem> canonicalItems = new LinkedHashMap<>();
        Map<String, Integer> quantityByKey = new LinkedHashMap<>();

        for (CartItem item : cartItemRepository.findByUserIdOrderByIdAsc(userId)) {
            String key = item.getProduct().getId() + "_" + (item.getProductVariant() != null ? item.getProductVariant().getId() : "null");
            canonicalItems.putIfAbsent(key, item);
            quantityByKey.merge(key, getQuantityOrZero(item), Integer::sum);
        }

        List<CartItemResponse> responses = new ArrayList<>();
        for (Map.Entry<String, CartItem> entry : canonicalItems.entrySet()) {
            responses.add(toResponse(entry.getValue(), quantityByKey.get(entry.getKey())));
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
                .variant(toVariantResponse(item.getProductVariant()))
                .build();
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant) {
        if (variant == null) return null;
        Map<String, String> attrs = new LinkedHashMap<>();
        if (variant.getAttributeValues() != null) {
            for (AttributeValue av : variant.getAttributeValues()) {
                attrs.put(av.getAttribute().getName(), av.getValue());
            }
        }
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .originalPrice(variant.getOriginalPrice())
                .stockQuantity(variant.getStockQuantity())
                .lowStockThreshold(variant.getLowStockThreshold())
                .available(variant.getAvailable() == null || variant.getAvailable())
                .attributes(attrs)
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

    private void validateProductOrVariantAvailability(Product product, ProductVariant variant) {
        if (variant != null) {
            if (Boolean.FALSE.equals(variant.getAvailable()) || variant.getStockQuantity() == null || variant.getStockQuantity() < 1) {
                throw new BadRequestException("This variant is currently out of stock");
            }
        } else {
            if (!product.isAvailable() || product.getStockQuantity() == null || product.getStockQuantity() < 1) {
                throw new BadRequestException("This product is currently out of stock");
            }
        }
    }

    private void validateProductOrVariantQuantity(Product product, ProductVariant variant, int quantity) {
        if (variant != null) {
            if (variant.getStockQuantity() != null && quantity > variant.getStockQuantity()) {
                throw new BadRequestException("Requested quantity exceeds variant stock");
            }
        } else {
            if (product.getStockQuantity() != null && quantity > product.getStockQuantity()) {
                throw new BadRequestException("Requested quantity exceeds available stock");
            }
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
