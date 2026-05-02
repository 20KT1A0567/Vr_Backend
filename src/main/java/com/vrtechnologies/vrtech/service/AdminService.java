package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.CouponRequest;
import com.vrtechnologies.vrtech.dto.request.SiteSettingsRequest;
import com.vrtechnologies.vrtech.dto.response.DashboardStatsResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardLowStockResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardOrderStatusResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardRecentOrderResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardStoreSalesResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardTopProductResponse;
import com.vrtechnologies.vrtech.dto.response.UserSummaryResponse;
import com.vrtechnologies.vrtech.entity.Coupon;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CouponRepository;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.EnquiryRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.SiteSettingsRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final EnquiryRepository enquiryRepository;
    private final StoreRepository storeRepository;
    private final CouponRepository couponRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final PermissionService permissionService;

    public AdminService(
            ProductRepository productRepository,
            UserRepository userRepository,
            CustomerOrderRepository customerOrderRepository,
            EnquiryRepository enquiryRepository,
            StoreRepository storeRepository,
            CouponRepository couponRepository,
            SiteSettingsRepository siteSettingsRepository,
            PermissionService permissionService
    ) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.enquiryRepository = enquiryRepository;
        this.storeRepository = storeRepository;
        this.couponRepository = couponRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.permissionService = permissionService;
    }

    public DashboardStatsResponse getDashboardStats(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        List<CustomerOrder> orders = customerOrderRepository.findAll().stream()
                .filter(order -> accessibleStoreIds.isEmpty() || (order.getStore() != null && accessibleStoreIds.contains(order.getStore().getId())))
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
                .map(order -> order.getUser().getId())
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
        settings.setSupportEmail(normalizeString(request.getSupportEmail()));
        settings.setSupportPhone(normalizeString(request.getSupportPhone()));
        settings.setShippingNote(normalizeString(request.getShippingNote()));
        settings.setReturnPolicy(normalizeString(request.getReturnPolicy()));
        settings.setDefaultCity(normalizeString(request.getDefaultCity()));
        settings.setDefaultState(normalizeString(request.getDefaultState()));
        settings.setMapLink(normalizeString(request.getMapLink()));
        return siteSettingsRepository.save(settings);
    }

    private UserSummaryResponse toResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
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

    private double roundPercentage(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private SiteSettings createDefaultSettings() {
        Store primaryStore = storeRepository.findAllByOrderByCityAscNameAsc().stream().findFirst().orElse(null);
        User adminUser = userRepository.findAll().stream().filter(User::isAdmin).findFirst().orElse(null);

        SiteSettings settings = new SiteSettings();
        settings.setCompanyName("VR Technologies");
        settings.setSupportEmail(adminUser != null ? adminUser.getEmail() : "superadmin@vrtechnologies.com");
        settings.setSupportPhone(primaryStore != null ? primaryStore.getPhone() : "");
        settings.setShippingNote("Standard delivery in 3-5 working days.");
        settings.setReturnPolicy("7-day easy returns on eligible products.");
        settings.setDefaultCity(primaryStore != null ? primaryStore.getCity() : "Hyderabad");
        settings.setDefaultState(primaryStore != null ? primaryStore.getState() : "Telangana");
        settings.setMapLink(primaryStore != null ? primaryStore.getMapLink() : null);
        return siteSettingsRepository.save(settings);
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
