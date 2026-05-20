package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Customer360Response {

    private final Profile profile;
    private final Summary summary;
    private final List<OrderItem> recentOrders;
    private final List<CartLine> cart;
    private final List<WishlistLine> wishlist;
    private final List<EnquiryLine> enquiries;
    private final List<ReturnLine> returns;
    private final List<BackInStockLine> backInStock;
    private final List<AddressLine> addresses;

    @Getter
    @Builder
    public static class Profile {
        private final Long id;
        private final String name;
        private final String email;
        private final String phone;
        private final String role;
        private final boolean active;
        private final String profileImageUrl;
        private final LocalDateTime registeredAt;
        private final LocalDateTime lastLoginAt;
    }

    @Getter
    @Builder
    public static class Summary {
        private final long totalOrders;
        private final long completedOrders;
        private final long cancelledOrders;
        private final BigDecimal lifetimeSpend;
        private final BigDecimal averageOrderValue;
        private final LocalDateTime firstOrderAt;
        private final LocalDateTime lastOrderAt;
        private final long openEnquiries;
        private final long openReturns;
        private final long cartItemCount;
        private final long wishlistItemCount;
        private final long backInStockSubscriptions;
    }

    @Getter
    @Builder
    public static class OrderItem {
        private final Long id;
        private final String orderNumber;
        private final OrderStatus status;
        private final PaymentStatus paymentStatus;
        private final BigDecimal totalAmount;
        private final int itemCount;
        private final String storeName;
        private final LocalDateTime placedAt;
        private final LocalDateTime paidAt;
        private final LocalDateTime deliveredAt;
        private final LocalDateTime cancelledAt;
    }

    @Getter
    @Builder
    public static class CartLine {
        private final Long id;
        private final Long productId;
        private final String productTitle;
        private final String productImageUrl;
        private final BigDecimal price;
        private final int quantity;
        private final BigDecimal lineTotal;
        private final LocalDateTime addedAt;
    }

    @Getter
    @Builder
    public static class WishlistLine {
        private final Long id;
        private final Long productId;
        private final String productTitle;
        private final String productImageUrl;
        private final BigDecimal price;
        private final boolean inStock;
        private final LocalDateTime addedAt;
    }

    @Getter
    @Builder
    public static class EnquiryLine {
        private final Long id;
        private final String enquiryType;
        private final String status;
        private final String message;
        private final Long productId;
        private final String productTitle;
        private final LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class ReturnLine {
        private final Long id;
        private final Long orderId;
        private final String orderNumber;
        private final ReturnRequestStatus status;
        private final String reason;
        private final LocalDateTime createdAt;
        private final LocalDateTime resolvedAt;
    }

    @Getter
    @Builder
    public static class BackInStockLine {
        private final Long id;
        private final Long productId;
        private final String productTitle;
        private final String status;
        private final LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class AddressLine {
        private final Long id;
        private final String label;
        private final String fullName;
        private final String phone;
        private final String addressLine1;
        private final String addressLine2;
        private final String city;
        private final String state;
        private final String pincode;
        private final boolean defaultAddress;
    }
}
