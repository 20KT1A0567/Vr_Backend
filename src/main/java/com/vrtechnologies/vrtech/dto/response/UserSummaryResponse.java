package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private String preferredContactName;
    private String preferredContactPhone;
    private String preferredContactEmail;
    private String defaultDeliveryAddress;
    private long ordersCount;
    private long deliveredOrdersCount;
    private long pendingOrdersCount;
    private BigDecimal totalSpent;
    private LocalDateTime lastOrderAt;
    private String lastOrderStatus;
    private String lastPaymentStatus;
    private long cartItemCount;
    private long cartQuantity;
    private long wishlistCount;
}
