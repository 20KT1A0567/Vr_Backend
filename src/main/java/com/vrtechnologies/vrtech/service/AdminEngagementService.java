package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.AdminCartItemResponse;
import com.vrtechnologies.vrtech.dto.response.AdminWishlistItemResponse;
import com.vrtechnologies.vrtech.dto.response.CartRecoveryResponse;
import com.vrtechnologies.vrtech.dto.response.UserSummaryResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.WishlistItem;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminEngagementService {

    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final PermissionService permissionService;
    private final ProductService productService;
    private final NotificationService notificationService;

    public AdminEngagementService(
            CartItemRepository cartItemRepository,
            WishlistItemRepository wishlistItemRepository,
            PermissionService permissionService,
            ProductService productService,
            NotificationService notificationService
    ) {
        this.cartItemRepository = cartItemRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.permissionService = permissionService;
        this.productService = productService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AdminCartItemResponse> getCartItems(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return cartItemRepository.findAllByOrderByIdDesc().stream()
                .filter(item -> canAccessProduct(accessibleStoreIds, item.getProduct()))
                .map(this::toCartResponse)
                .toList();
    }

    @Transactional
    public void deleteCartItem(User admin, Long id) {
        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!canAccessProduct(permissionService.accessibleStoreIds(admin), item.getProduct())) {
            throw new ResourceNotFoundException("Cart item not found");
        }
        cartItemRepository.delete(item);
    }

    @Transactional
    public CartRecoveryResponse recoverCartItem(User admin, Long id) {
        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!canAccessProduct(permissionService.accessibleStoreIds(admin), item.getProduct())) {
            throw new ResourceNotFoundException("Cart item not found");
        }
        User customer = item.getUser();
        Product product = item.getProduct();
        String subject = "Complete your VR Technologies cart";
        String message = "Customer " + customer.getName() + " has " + item.getQuantity() + " x " + product.getTitle()
                + " in cart. Follow up and help complete checkout.";
        var emailLog = notificationService.log("CART_ABANDONMENT_RECOVERY", "EMAIL", publicEmail(customer), subject, message, null);
        var whatsappLog = notificationService.log("CART_ABANDONMENT_RECOVERY", "WHATSAPP", customer.getPhone(), subject, message, null);
        return CartRecoveryResponse.builder()
                .cartItemId(item.getId())
                .userId(customer.getId())
                .customerName(customer.getName())
                .emailStatus(emailLog != null ? emailLog.getStatus() : "SKIPPED")
                .whatsappStatus(whatsappLog != null ? whatsappLog.getStatus() : "SKIPPED")
                .message("Recovery queued for " + customer.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminWishlistItemResponse> getWishlistItems(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return wishlistItemRepository.findAllByOrderByAddedAtDescIdDesc().stream()
                .filter(item -> canAccessProduct(accessibleStoreIds, item.getProduct()))
                .map(this::toWishlistResponse)
                .toList();
    }

    @Transactional
    public void deleteWishlistItem(User admin, Long id) {
        WishlistItem item = wishlistItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        if (!canAccessProduct(permissionService.accessibleStoreIds(admin), item.getProduct())) {
            throw new ResourceNotFoundException("Wishlist item not found");
        }
        wishlistItemRepository.delete(item);
    }

    private AdminCartItemResponse toCartResponse(CartItem item) {
        return AdminCartItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .estimatedValue((item.getProduct().getPrice() == null ? java.math.BigDecimal.ZERO : item.getProduct().getPrice())
                        .multiply(java.math.BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity())))
                .recoverable(hasRecoveryContact(item.getUser()))
                .user(toUserSummary(item.getUser()))
                .product(productService.toProductResponse(item.getProduct()))
                .build();
    }

    private boolean hasRecoveryContact(User user) {
        return user != null && ((publicEmail(user) != null && !publicEmail(user).isBlank()) || (user.getPhone() != null && !user.getPhone().isBlank()));
    }

    private AdminWishlistItemResponse toWishlistResponse(WishlistItem item) {
        return AdminWishlistItemResponse.builder()
                .id(item.getId())
                .addedAt(item.getAddedAt())
                .user(toUserSummary(item.getUser()))
                .product(productService.toProductResponse(item.getProduct()))
                .build();
    }

    private UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(publicEmail(user))
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String publicEmail(User user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        if (user.getRole() == com.vrtechnologies.vrtech.entity.enums.Role.USER && isInternalPhoneLoginEmail(user)) {
            return null;
        }
        return user.getEmail();
    }

    private boolean isInternalPhoneLoginEmail(User user) {
        String email = user.getEmail().toLowerCase(java.util.Locale.ROOT);
        String phone = user.getPhone() == null ? "" : user.getPhone().toLowerCase(java.util.Locale.ROOT);
        return email.endsWith("@phone.anushabazaar.local") || (!phone.isBlank() && email.equals(phone));
    }

    private boolean canAccessProduct(List<Long> accessibleStoreIds, Product product) {
        return accessibleStoreIds.isEmpty()
                || product.getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains);
    }
}
