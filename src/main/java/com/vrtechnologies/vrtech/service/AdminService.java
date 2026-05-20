package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.CouponRequest;
import com.vrtechnologies.vrtech.dto.request.SiteSettingsRequest;
import com.vrtechnologies.vrtech.dto.response.DashboardStatsResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardLowStockResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardOrderStatusResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardRecentOrderResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardStoreSalesResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardTopProductResponse;
import com.vrtechnologies.vrtech.dto.response.AdminStorePerformanceResponse;
import com.vrtechnologies.vrtech.dto.response.AdminStoreTopProductResponse;
import com.vrtechnologies.vrtech.dto.response.AdminActivitySummaryResponse;
import com.vrtechnologies.vrtech.dto.response.CouponAnalyticsResponse;
import com.vrtechnologies.vrtech.dto.response.UserSummaryResponse;
import com.vrtechnologies.vrtech.entity.Coupon;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.AdminActivityLogRepository;
import com.vrtechnologies.vrtech.repository.AdminLoginHistoryRepository;
import com.vrtechnologies.vrtech.repository.CouponRepository;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.EnquiryRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ReturnRequestRepository;
import com.vrtechnologies.vrtech.repository.SiteSettingsRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import com.vrtechnologies.vrtech.repository.WishlistItemRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final CartItemRepository cartItemRepository;
    private final EnquiryRepository enquiryRepository;
    private final StoreRepository storeRepository;
    private final CouponRepository couponRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final PermissionService permissionService;
    private final AdminActivityLogRepository adminActivityLogRepository;
    private final AdminLoginHistoryRepository adminLoginHistoryRepository;
    private final ReturnRequestRepository returnRequestRepository;

    public AdminService(
            ProductRepository productRepository,
            UserRepository userRepository,
            CustomerOrderRepository customerOrderRepository,
            CartItemRepository cartItemRepository,
            EnquiryRepository enquiryRepository,
            StoreRepository storeRepository,
            CouponRepository couponRepository,
            SiteSettingsRepository siteSettingsRepository,
            WishlistItemRepository wishlistItemRepository,
            PermissionService permissionService,
            AdminActivityLogRepository adminActivityLogRepository,
            AdminLoginHistoryRepository adminLoginHistoryRepository,
            ReturnRequestRepository returnRequestRepository
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.cartItemRepository = cartItemRepository;
        this.enquiryRepository = enquiryRepository;
        this.storeRepository = storeRepository;
        this.couponRepository = couponRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.permissionService = permissionService;
        this.adminActivityLogRepository = adminActivityLogRepository;
        this.adminLoginHistoryRepository = adminLoginHistoryRepository;
        this.returnRequestRepository = returnRequestRepository;
    }

    public DashboardStatsResponse getDashboardStats(User admin) {
        return getDashboardStats(admin, "ALL_TIME");
    }

    public DashboardStatsResponse getDashboardStats(User admin, String period) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        List<CustomerOrder> scopedOrders = customerOrderRepository.findAll().stream()
                .filter(order -> accessibleStoreIds.isEmpty() || (order.getStore() != null && accessibleStoreIds.contains(order.getStore().getId())))
                .toList();
        List<CustomerOrder> orders = scopedOrders.stream()
                .filter(order -> isInsideReportPeriod(order, period))
                .toList();
        List<Product> products = productRepository.findAll().stream()
                .filter(product -> accessibleStoreIds.isEmpty()
                        || product.getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains))
                .toList();
        List<Store> stores = storeRepository.findAllByOrderByCityAscNameAsc().stream()
                .filter(store -> accessibleStoreIds.isEmpty() || accessibleStoreIds.contains(store.getId()))
                .toList();
        List<com.vrtechnologies.vrtech.entity.Enquiry> enquiries = enquiryRepository.findAll().stream()
                .filter(enquiry -> accessibleStoreIds.isEmpty()
                        || (enquiry.getProduct() != null
                        && enquiry.getProduct().getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains)))
                .toList();

        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED && order.getPaymentStatus() != com.vrtechnologies.vrtech.entity.enums.PaymentStatus.REFUNDED)
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardOrderStatusResponse> orderStatuses = buildOrderStatusBreakdown(orders);
        List<DashboardStoreSalesResponse> storeSales = buildStoreSales(stores, products, orders);
        List<DashboardTopProductResponse> topProducts = buildTopProducts(orders);
        List<DashboardLowStockResponse> lowStockItems = buildLowStockItems(products);
        long lowStockProductsCount = products.stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() <= 5)
                .count();
        List<DashboardRecentOrderResponse> recentOrders = buildRecentOrders(orders);
        long totalUsers = orders.stream()
                .map(CustomerOrder::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(id -> id != null)
                .distinct()
                .count();
        long pendingOrders = orders.stream()
                .filter(order -> switch (order.getStatus()) {
                    case PENDING, CONFIRMED, PACKED, SHIPPED, READY -> true;
                    default -> false;
                })
                .count();
        long newEnquiries = enquiries.stream().filter(enquiry -> enquiry.getStatus() == EnquiryStatus.NEW).count();

        return DashboardStatsResponse.builder()
                .totalProducts(products.size())
                .totalUsers(totalUsers)
                .totalOrders(orders.size())
                .totalStores(stores.size())
                .activeStores(stores.stream().filter(Store::isActive).count())
                .totalRevenue(totalRevenue)
                .newEnquiries(newEnquiries)
                .pendingOrders(pendingOrders)
                .lowStockProducts(lowStockProductsCount)
                .orderStatuses(orderStatuses)
                .storeSales(storeSales)
                .topProducts(topProducts)
                .lowStockItems(lowStockItems)
                .recentOrders(recentOrders)
                .build();
    }

    public List<UserSummaryResponse> getUsers(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return userRepository.findAll().stream()
                .filter(user -> canAccessUser(accessibleStoreIds, user))
                .map(this::toResponse)
                .toList();
    }

    public List<AdminStorePerformanceResponse> getStorePerformance(User admin) {
        return getStorePerformance(admin, "ALL_TIME");
    }

    public List<AdminStorePerformanceResponse> getStorePerformance(User admin, String period) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        List<Store> stores = storeRepository.findAllByOrderByCityAscNameAsc().stream()
                .filter(store -> accessibleStoreIds.isEmpty() || accessibleStoreIds.contains(store.getId()))
                .toList();
        List<Product> products = productRepository.findAll().stream()
                .filter(product -> accessibleStoreIds.isEmpty()
                        || product.getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains))
                .toList();
        List<CustomerOrder> orders = customerOrderRepository.findAll().stream()
                .filter(order -> accessibleStoreIds.isEmpty() || (order.getStore() != null && accessibleStoreIds.contains(order.getStore().getId())))
                .filter(order -> isInsideReportPeriod(order, period))
                .toList();
        BigDecimal totalRevenue = orders.stream()
                .filter(this::isRevenueOrder)
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stores.stream()
                .map(store -> buildStorePerformance(store, products, orders, totalRevenue))
                .sorted(Comparator.comparing(AdminStorePerformanceResponse::getRevenue, Comparator.reverseOrder())
                        .thenComparing(AdminStorePerformanceResponse::getOrdersCount, Comparator.reverseOrder())
                        .thenComparing(AdminStorePerformanceResponse::getStoreName))
                .toList();
    }

    private boolean isInsideReportPeriod(CustomerOrder order, String period) {
        LocalDateTime start = resolveReportPeriodStart(period);
        if (start == null) {
            return true;
        }
        LocalDateTime createdAt = order.getCreatedAt();
        return createdAt != null && !createdAt.isBefore(start);
    }

    private LocalDateTime resolveReportPeriodStart(String period) {
        String normalized = period == null ? "ALL_TIME" : period.trim().toUpperCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        return switch (normalized) {
            case "TODAY", "DAY" -> today.atStartOfDay();
            case "WEEK", "THIS_WEEK" -> today.with(DayOfWeek.MONDAY).atStartOfDay();
            case "MONTH", "THIS_MONTH" -> today.withDayOfMonth(1).atStartOfDay();
            default -> null;
        };
    }

    public UserSummaryResponse toggleUser(User admin, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!canAccessUser(permissionService.accessibleStoreIds(admin), user)) {
            throw new ResourceNotFoundException("User not found");
        }
        user.setActive(!user.isActive());
        return toResponse(userRepository.save(user));
    }

    public List<Coupon> getCoupons() {
        return couponRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
    }

    public List<CouponAnalyticsResponse> getCouponAnalytics() {
        return getCoupons().stream()
                .map(coupon -> CouponAnalyticsResponse.builder()
                        .couponId(coupon.getId())
                        .code(coupon.getCode())
                        .usageCount(coupon.getUsageCount() == null ? 0 : coupon.getUsageCount())
                        .orderCount(customerOrderRepository.findAll().stream()
                                .filter(order -> coupon.getCode() != null && coupon.getCode().equalsIgnoreCase(order.getCouponCode()))
                                .count())
                        .discountGiven(coupon.getTotalDiscountGiven() == null ? BigDecimal.ZERO : coupon.getTotalDiscountGiven())
                        .revenueAfterDiscount(coupon.getTotalRevenueGenerated() == null ? BigDecimal.ZERO : coupon.getTotalRevenueGenerated())
                        .build())
                .toList();
    }

    public AdminActivitySummaryResponse getAdminActivitySummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long failedLogins = adminLoginHistoryRepository.countByStatusAndLoginAtAfter("FAILED", startOfDay);
        return AdminActivitySummaryResponse.builder()
                .todayChanges(adminActivityLogRepository.countByCreatedAtAfter(startOfDay))
                .failedLoginsToday(failedLogins)
                .suspiciousActionsToday(failedLogins)
                .openReturns(returnRequestRepository.countByStatusIn(List.of(
                        ReturnRequestStatus.REQUESTED,
                        ReturnRequestStatus.APPROVED,
                        ReturnRequestStatus.PICKUP_SCHEDULED,
                        ReturnRequestStatus.PICKED_UP,
                        ReturnRequestStatus.INSPECTED,
                        ReturnRequestStatus.REFUND_PENDING
                )))
                .failedPayments(customerOrderRepository.countByPaymentStatus(PaymentStatus.FAILED))
                .build();
    }

    public Coupon saveCoupon(CouponRequest request, Long id) {
        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        boolean codeExists = id == null
                ? couponRepository.existsByCodeIgnoreCase(normalizedCode)
                : couponRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id);
        if (codeExists) {
            throw new BadRequestException("Coupon code already exists");
        }

        Coupon coupon = id == null ? new Coupon() : couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setCode(normalizedCode);
        coupon.setDiscount(request.getDiscount());
        coupon.setMinOrder(request.getMinOrder());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStatus(request.getStatus());
        return couponRepository.save(coupon);
    }

    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        couponRepository.delete(coupon);
    }

    public SiteSettings getSiteSettings() {
        return siteSettingsRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
    }

    public SiteSettings saveSiteSettings(SiteSettingsRequest request) {
        SiteSettings settings = getSiteSettings();
        settings.setCompanyName(request.getCompanyName().trim());
        if (request.getLogoUrl() != null) {
            settings.setLogoUrl(normalizeString(request.getLogoUrl()));
        }
        if (request.getFaviconUrl() != null) {
            settings.setFaviconUrl(normalizeString(request.getFaviconUrl()));
        }
        if (request.getTagline() != null) {
            settings.setTagline(normalizeString(request.getTagline()));
        }
        if (request.getFooterDescription() != null) {
            settings.setFooterDescription(normalizeString(request.getFooterDescription()));
        }
        settings.setSupportEmail(normalizeString(request.getSupportEmail()));
        settings.setSupportPhone(normalizeString(request.getSupportPhone()));
        settings.setShippingNote(normalizeString(request.getShippingNote()));
        settings.setPickupEnabled(request.getPickupEnabled() == null || request.getPickupEnabled());
        settings.setDeliveryEnabled(request.getDeliveryEnabled() == null || request.getDeliveryEnabled());
        settings.setStandardDeliveryCharge(nonNegative(request.getStandardDeliveryCharge()));
        settings.setFreeDeliveryThreshold(request.getFreeDeliveryThreshold() == null ? null : nonNegative(request.getFreeDeliveryThreshold()));
        settings.setStateDeliveryCharges(normalizeString(request.getStateDeliveryCharges()));
        settings.setStateDeliveryWindows(normalizeString(request.getStateDeliveryWindows()));
        settings.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays() == null || request.getEstimatedDeliveryDays() < 1 ? 5 : request.getEstimatedDeliveryDays());
        settings.setGstEnabled(request.getGstEnabled() == null || request.getGstEnabled());
        settings.setGstRate(nonNegative(request.getGstRate() == null ? BigDecimal.valueOf(18) : request.getGstRate()));
        settings.setGstNumber(normalizeString(request.getGstNumber()));
        settings.setCompanyPan(normalizeUpper(request.getCompanyPan()));
        settings.setDefaultHsnCode(normalizeUpper(request.getDefaultHsnCode()));
        settings.setCompanyAddress(normalizeString(request.getCompanyAddress()));
        settings.setCompanyPincode(normalizeString(request.getCompanyPincode()));
        if (request.getInvoicePrefix() != null) {
            String prefix = request.getInvoicePrefix().trim();
            settings.setInvoicePrefix(prefix.isEmpty() ? "INV-" : prefix);
        }
        if (request.getInvoicePadding() != null) {
            int padding = request.getInvoicePadding();
            settings.setInvoicePadding(Math.max(1, Math.min(12, padding)));
        }
        if (request.getInvoiceNextSequence() != null && request.getInvoiceNextSequence() > 0) {
            // Allow super-admin to reset/jump the sequence (e.g. fresh financial year)
            settings.setInvoiceNextSequence(request.getInvoiceNextSequence());
        }
        settings.setInvoiceTerms(normalizeString(request.getInvoiceTerms()));
        settings.setReturnPolicy(normalizeString(request.getReturnPolicy()));
        settings.setDefaultCity(normalizeString(request.getDefaultCity()));
        settings.setDefaultState(normalizeString(request.getDefaultState()));
        settings.setMapLink(normalizeString(request.getMapLink()));
        settings.setIncludeDefaultHomeSections(request.getIncludeDefaultHomeSections() == null || request.getIncludeDefaultHomeSections());
        settings.setDefaultHomeSectionTypes(request.getDefaultHomeSectionTypes() == null ? null : request.getDefaultHomeSectionTypes().trim());
        settings.setNotificationEmailFrom(normalizeString(request.getNotificationEmailFrom()));
        settings.setNotificationReplyTo(normalizeString(request.getNotificationReplyTo()));
        settings.setWhatsappNumber(normalizeString(request.getWhatsappNumber()));
        if (request.getFacebookUrl() != null) {
            settings.setFacebookUrl(normalizeString(request.getFacebookUrl()));
        }
        if (request.getInstagramUrl() != null) {
            settings.setInstagramUrl(normalizeString(request.getInstagramUrl()));
        }
        if (request.getXUrl() != null) {
            settings.setXUrl(normalizeString(request.getXUrl()));
        }
        if (request.getLinkedinUrl() != null) {
            settings.setLinkedinUrl(normalizeString(request.getLinkedinUrl()));
        }
        if (request.getYoutubeUrl() != null) {
            settings.setYoutubeUrl(normalizeString(request.getYoutubeUrl()));
        }
        settings.setHomepageBuilderJson(normalizeString(request.getHomepageBuilderJson()));
        settings.setOrderNotificationsEnabled(request.getOrderNotificationsEnabled() == null || request.getOrderNotificationsEnabled());
        settings.setPaymentNotificationsEnabled(request.getPaymentNotificationsEnabled() == null || request.getPaymentNotificationsEnabled());
        settings.setReturnNotificationsEnabled(request.getReturnNotificationsEnabled() == null || request.getReturnNotificationsEnabled());
        settings.setSecurityNotice(normalizeString(request.getSecurityNotice()));
        return siteSettingsRepository.save(settings);
    }

    private UserSummaryResponse toResponse(User user) {
        List<CustomerOrder> orders = customerOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        CustomerOrder lastOrder = orders.stream().findFirst().orElse(null);
        long pendingOrders = orders.stream()
                .filter(order -> switch (order.getStatus()) {
                    case PENDING, CONFIRMED, PACKED, SHIPPED, READY, RETURN_REQUESTED -> true;
                    default -> false;
                })
                .count();
        BigDecimal totalSpent = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED && order.getPaymentStatus() != com.vrtechnologies.vrtech.entity.enums.PaymentStatus.REFUNDED)
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(publicEmail(user))
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .preferredContactName(user.getPreferredContactName())
                .preferredContactPhone(user.getPreferredContactPhone())
                .preferredContactEmail(user.getPreferredContactEmail())
                .defaultDeliveryAddress(user.getPreferredDeliveryAddress())
                .ordersCount(orders.size())
                .deliveredOrdersCount(orders.stream().filter(order -> order.getStatus() == OrderStatus.DELIVERED).count())
                .pendingOrdersCount(pendingOrders)
                .totalSpent(totalSpent)
                .lastOrderAt(lastOrder != null ? lastOrder.getCreatedAt() : null)
                .lastOrderStatus(lastOrder != null && lastOrder.getStatus() != null ? lastOrder.getStatus().name() : null)
                .lastPaymentStatus(lastOrder != null && lastOrder.getPaymentStatus() != null ? lastOrder.getPaymentStatus().name() : null)
                .cartItemCount(cartItems.size())
                .cartQuantity(cartItems.stream().mapToLong(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum())
                .wishlistCount(wishlistItemRepository.findByUserIdOrderByAddedAtDesc(user.getId()).size())
                .build();
    }

    private String publicEmail(User user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        if (user.getRole() == Role.USER && isInternalPhoneLoginEmail(user)) {
            return null;
        }
        return user.getEmail();
    }

    private boolean isInternalPhoneLoginEmail(User user) {
        String email = user.getEmail().toLowerCase(Locale.ROOT);
        String phone = user.getPhone() == null ? "" : user.getPhone().toLowerCase(Locale.ROOT);
        return email.endsWith("@phone.anushabazaar.local") || (!phone.isBlank() && email.equals(phone));
    }

    private List<DashboardOrderStatusResponse> buildOrderStatusBreakdown(List<CustomerOrder> orders) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }

        for (CustomerOrder order : orders) {
            counts.computeIfPresent(order.getStatus(), (status, count) -> count + 1);
        }

        double totalOrders = Math.max(orders.size(), 1);
        return counts.entrySet().stream()
                .map(entry -> DashboardOrderStatusResponse.builder()
                        .status(entry.getKey().name())
                        .count(entry.getValue())
                        .percentage(roundPercentage((entry.getValue() * 100.0) / totalOrders))
                        .build())
                .toList();
    }

    private List<DashboardStoreSalesResponse> buildStoreSales(List<Store> stores, List<Product> products, List<CustomerOrder> orders) {
        Map<Long, StoreAccumulator> storeAccumulators = new LinkedHashMap<>();
        for (Store store : stores) {
            storeAccumulators.put(store.getId(), new StoreAccumulator(store));
        }

        for (Product product : products) {
            for (Store store : product.getStores()) {
                StoreAccumulator accumulator = storeAccumulators.get(store.getId());
                if (accumulator != null) {
                    accumulator.productsCount += 1;
                }
            }
        }

        for (CustomerOrder order : orders) {
            if (order.getStore() == null || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
                continue;
            }

            StoreAccumulator accumulator = storeAccumulators.get(order.getStore().getId());
            if (accumulator == null) {
                continue;
            }

            accumulator.ordersCount += 1;
            accumulator.revenue = accumulator.revenue.add(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        }

        return storeAccumulators.values().stream()
                .sorted(Comparator.comparing(StoreAccumulator::revenue).reversed().thenComparing(accumulator -> accumulator.store.getName()))
                .map(accumulator -> DashboardStoreSalesResponse.builder()
                        .storeId(accumulator.store.getId())
                        .storeName(accumulator.store.getName())
                        .ordersCount(accumulator.ordersCount)
                        .revenue(accumulator.revenue)
                        .productsCount(accumulator.productsCount)
                        .active(accumulator.store.isActive())
                        .build())
                .toList();
    }

    private List<DashboardTopProductResponse> buildTopProducts(List<CustomerOrder> orders) {
        Map<Long, ProductAccumulator> productAccumulators = new HashMap<>();
        for (CustomerOrder order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
                continue;
            }

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                ProductAccumulator accumulator = productAccumulators.computeIfAbsent(product.getId(), ignored -> new ProductAccumulator(product));
                accumulator.soldQuantity += item.getQuantity();
                accumulator.revenue = accumulator.revenue.add(item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        return productAccumulators.values().stream()
                .sorted((left, right) -> {
                    int quantityCompare = Long.compare(right.soldQuantity, left.soldQuantity);
                    if (quantityCompare != 0) {
                        return quantityCompare;
                    }
                    return right.revenue.compareTo(left.revenue);
                })
                .limit(6)
                .map(accumulator -> DashboardTopProductResponse.builder()
                        .productId(accumulator.product.getId())
                        .title(accumulator.product.getTitle())
                        .soldQuantity(accumulator.soldQuantity)
                        .revenue(accumulator.revenue)
                        .stockQuantity(accumulator.product.getStockQuantity())
                        .storeNames(accumulator.product.getStores().stream().map(Store::getName).sorted().toList())
                        .build())
                .toList();
    }

    private List<DashboardLowStockResponse> buildLowStockItems(List<Product> products) {
        return products.stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() <= 5)
                .sorted(Comparator.comparing(Product::getStockQuantity).thenComparing(Product::getTitle))
                .limit(8)
                .map(product -> DashboardLowStockResponse.builder()
                        .productId(product.getId())
                        .title(product.getTitle())
                        .stockQuantity(product.getStockQuantity())
                        .available(product.isAvailable())
                        .storeNames(product.getStores().stream().map(Store::getName).sorted().toList())
                        .build())
                .toList();
    }

    private List<DashboardRecentOrderResponse> buildRecentOrders(List<CustomerOrder> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(CustomerOrder::getCreatedAt).reversed())
                .limit(6)
                .map(order -> DashboardRecentOrderResponse.builder()
                        .orderId(order.getId())
                        .customerName(order.getContactName())
                        .contactPhone(order.getContactPhone())
                        .storeName(order.getStore() != null ? order.getStore().getName() : "Unassigned")
                        .amount(order.getTotalAmount())
                        .status(order.getStatus())
                        .paymentStatus(order.getPaymentStatus())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();
    }

    private AdminStorePerformanceResponse buildStorePerformance(Store store, List<Product> products, List<CustomerOrder> orders, BigDecimal totalRevenue) {
        List<Product> storeProducts = products.stream()
                .filter(product -> product.getStores().stream().map(Store::getId).anyMatch(store.getId()::equals))
                .toList();
        List<CustomerOrder> storeOrders = orders.stream()
                .filter(order -> order.getStore() != null && store.getId().equals(order.getStore().getId()))
                .toList();

        BigDecimal revenue = storeOrders.stream()
                .filter(this::isRevenueOrder)
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pipelineRevenue = storeOrders.stream()
                .filter(this::isPipelineOrder)
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long deliveredOrders = storeOrders.stream().filter(order -> order.getStatus() == OrderStatus.DELIVERED).count();
        long cancelledOrders = storeOrders.stream().filter(order -> order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED).count();
        long unitsSold = storeOrders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.REFUNDED)
                .flatMap(order -> order.getItems().stream())
                .mapToLong(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
        BigDecimal averageOrderValue = deliveredOrders == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(deliveredOrders), 2, RoundingMode.HALF_UP);
        List<AdminStoreTopProductResponse> topProducts = buildStoreTopProducts(storeOrders);
        BigDecimal topProductRevenue = topProducts.stream().findFirst()
                .map(AdminStoreTopProductResponse::getRevenue)
                .orElse(BigDecimal.ZERO);
        BigDecimal revenueContribution = totalRevenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : revenue.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP);
        BigDecimal activeProducts = BigDecimal.valueOf(Math.max(1, storeProducts.stream().filter(Product::isAvailable).count()));
        BigDecimal orderCount = BigDecimal.valueOf(Math.max(1, storeOrders.size()));
        BigDecimal topProductShare = revenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : topProductRevenue.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP);

        return AdminStorePerformanceResponse.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .city(store.getCity())
                .active(store.isActive())
                .productsCount(storeProducts.size())
                .activeProductsCount(storeProducts.stream().filter(Product::isAvailable).count())
                .lowStockProductsCount(storeProducts.stream()
                        .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() <= product.getResolvedLowStockThreshold())
                        .count())
                .ordersCount(storeOrders.size())
                .pendingOrdersCount(storeOrders.stream().filter(this::isPipelineOrder).count())
                .deliveredOrdersCount(deliveredOrders)
                .cancelledOrdersCount(cancelledOrders)
                .unitsSold(unitsSold)
                .revenue(revenue)
                .pipelineRevenue(pipelineRevenue)
                .averageOrderValue(averageOrderValue)
                .revenueContributionPercent(revenueContribution)
                .revenuePerActiveProduct(revenue.divide(activeProducts, 2, RoundingMode.HALF_UP))
                .unitsPerOrder(BigDecimal.valueOf(unitsSold).divide(orderCount, 2, RoundingMode.HALF_UP))
                .deliveredRate(storeOrders.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(deliveredOrders).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(storeOrders.size()), 2, RoundingMode.HALF_UP))
                .cancellationRate(storeOrders.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(cancelledOrders).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(storeOrders.size()), 2, RoundingMode.HALF_UP))
                .topProductRevenueShare(topProductShare)
                .lastOrderAt(storeOrders.stream()
                        .map(CustomerOrder::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null))
                .topProducts(topProducts)
                .build();
    }

    private List<AdminStoreTopProductResponse> buildStoreTopProducts(List<CustomerOrder> orders) {
        Map<Long, ProductAccumulator> productAccumulators = new HashMap<>();
        for (CustomerOrder order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
                continue;
            }

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                ProductAccumulator accumulator = productAccumulators.computeIfAbsent(product.getId(), ignored -> new ProductAccumulator(product));
                accumulator.soldQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
                BigDecimal lineRevenue = item.getPriceAtTime() == null
                        ? BigDecimal.ZERO
                        : item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));
                accumulator.revenue = accumulator.revenue.add(lineRevenue);
            }
        }

        return productAccumulators.values().stream()
                .sorted((left, right) -> {
                    int quantityCompare = Long.compare(right.soldQuantity, left.soldQuantity);
                    if (quantityCompare != 0) {
                        return quantityCompare;
                    }
                    return right.revenue.compareTo(left.revenue);
                })
                .limit(3)
                .map(accumulator -> AdminStoreTopProductResponse.builder()
                        .productId(accumulator.product.getId())
                        .title(accumulator.product.getTitle())
                        .soldQuantity(accumulator.soldQuantity)
                        .revenue(accumulator.revenue)
                        .stockQuantity(accumulator.product.getStockQuantity())
                        .available(accumulator.product.isAvailable())
                        .build())
                .toList();
    }

    private boolean isRevenueOrder(CustomerOrder order) {
        return order.getStatus() == OrderStatus.DELIVERED
                && order.getPaymentStatus() != com.vrtechnologies.vrtech.entity.enums.PaymentStatus.REFUNDED;
    }

    private boolean isPipelineOrder(CustomerOrder order) {
        return switch (order.getStatus()) {
            case PENDING, CONFIRMED, PACKED, SHIPPED, READY, RETURN_REQUESTED -> true;
            default -> false;
        };
    }

    private double roundPercentage(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private SiteSettings createDefaultSettings() {
        Store primaryStore = storeRepository.findAllByOrderByCityAscNameAsc().stream().findFirst().orElse(null);
        User adminUser = userRepository.findAll().stream().filter(User::isAdmin).findFirst().orElse(null);

        SiteSettings settings = new SiteSettings();
        settings.setCompanyName("VR Technologies");
        settings.setTagline("Refurbished. Warranted. Trusted.");
        settings.setFooterDescription("Certified refurbished laptops and desktops with warranty, quality checks, and store-backed support across Hyderabad.");
        settings.setSupportEmail(adminUser != null ? adminUser.getEmail() : "superadmin@vrtechnologies.com");
        settings.setSupportPhone(primaryStore != null ? primaryStore.getPhone() : "");
        settings.setShippingNote("Standard delivery in 3-5 working days.");
        settings.setPickupEnabled(true);
        settings.setDeliveryEnabled(true);
        settings.setStandardDeliveryCharge(BigDecimal.ZERO);
        settings.setFreeDeliveryThreshold(null);
        settings.setStateDeliveryCharges(null);
        settings.setStateDeliveryWindows("Telangana=2-3\nAndhra Pradesh=3-5\nKarnataka=4-6\nTamil Nadu=4-6\nMaharashtra=5-7");
        settings.setEstimatedDeliveryDays(5);
        settings.setGstEnabled(true);
        settings.setGstRate(BigDecimal.valueOf(18));
        settings.setReturnPolicy("7-day easy returns on eligible products.");
        settings.setDefaultCity(primaryStore != null ? primaryStore.getCity() : "Hyderabad");
        settings.setDefaultState(primaryStore != null ? primaryStore.getState() : "Telangana");
        settings.setMapLink(primaryStore != null ? primaryStore.getMapLink() : null);
        settings.setIncludeDefaultHomeSections(true);
        settings.setNotificationEmailFrom(adminUser != null ? adminUser.getEmail() : "support@vrtechnologies.com");
        settings.setNotificationReplyTo(adminUser != null ? adminUser.getEmail() : "support@vrtechnologies.com");
        settings.setWhatsappNumber(primaryStore != null ? primaryStore.getWhatsapp() : null);
        settings.setSecurityNotice("Use strong passwords and rotate production API credentials regularly.");
        return siteSettingsRepository.save(settings);
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpper(String value) {
        String trimmed = normalizeString(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean canAccessUser(List<Long> accessibleStoreIds, User user) {
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return true;
        }
        if (user == null || user.getRole() != Role.USER) {
            return false;
        }
        return customerOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(CustomerOrder::getStore)
                .filter(Objects::nonNull)
                .map(Store::getId)
                .anyMatch(accessibleStoreIds::contains);
    }

    private static final class StoreAccumulator {
        private final Store store;
        private long ordersCount;
        private long productsCount;
        private BigDecimal revenue = BigDecimal.ZERO;

        private StoreAccumulator(Store store) {
            this.store = store;
        }

        private BigDecimal revenue() {
            return revenue;
        }
    }

    private static final class ProductAccumulator {
        private final Product product;
        private long soldQuantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductAccumulator(Product product) {
            this.product = product;
        }
    }
}
