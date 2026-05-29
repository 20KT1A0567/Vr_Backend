package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.BannerRequest;
import com.vrtechnologies.vrtech.dto.request.BrandRequest;
import com.vrtechnologies.vrtech.dto.request.CategoryRequest;
import com.vrtechnologies.vrtech.dto.request.CouponRequest;
import com.vrtechnologies.vrtech.dto.request.OrderActionRequest;
import com.vrtechnologies.vrtech.dto.request.PincodeDeliveryRuleRequest;
import com.vrtechnologies.vrtech.dto.request.ProductReviewRequest;
import com.vrtechnologies.vrtech.dto.request.ReturnDecisionRequest;
import com.vrtechnologies.vrtech.dto.request.ReturnPickupRequest;
import com.vrtechnologies.vrtech.dto.request.ShipmentUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.SiteSettingsRequest;
import com.vrtechnologies.vrtech.dto.request.StatusUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.StockAdjustmentRequest;
import com.vrtechnologies.vrtech.dto.request.StockTransferRequest;
import com.vrtechnologies.vrtech.dto.request.StoreRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.AdminCartItemResponse;
import com.vrtechnologies.vrtech.dto.response.AdminActivitySummaryResponse;
import com.vrtechnologies.vrtech.dto.response.AdminStorePerformanceResponse;
import com.vrtechnologies.vrtech.dto.response.CouponAnalyticsResponse;
import com.vrtechnologies.vrtech.dto.response.Customer360Response;
import com.vrtechnologies.vrtech.dto.response.ExportManifestResponse;
import com.vrtechnologies.vrtech.dto.response.AdminWishlistItemResponse;
import com.vrtechnologies.vrtech.dto.response.BannerResponse;
import com.vrtechnologies.vrtech.dto.response.BackInStockRequestResponse;
import com.vrtechnologies.vrtech.dto.response.CartRecoveryResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardStatsResponse;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.OrderTimelineEventResponse;
import com.vrtechnologies.vrtech.dto.response.PincodeDeliveryRuleResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentRecoveryResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentWebhookEventResponse;
import com.vrtechnologies.vrtech.dto.response.ProductImportResponse;
import com.vrtechnologies.vrtech.dto.response.ProductReviewResponse;
import com.vrtechnologies.vrtech.dto.response.RazorpaySettingsResponse;
import com.vrtechnologies.vrtech.dto.response.RefundTransactionResponse;
import com.vrtechnologies.vrtech.dto.response.ReturnRequestResponse;
import com.vrtechnologies.vrtech.dto.response.StockMovementResponse;
import com.vrtechnologies.vrtech.dto.response.StockTransferResponse;
import com.vrtechnologies.vrtech.dto.response.SystemHealthResponse;
import com.vrtechnologies.vrtech.dto.response.UserSummaryResponse;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Coupon;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.PincodeZone;
import com.vrtechnologies.vrtech.entity.PincodeBlacklist;
import com.vrtechnologies.vrtech.entity.Holiday;
import com.vrtechnologies.vrtech.entity.PincodeLookupLog;
import com.vrtechnologies.vrtech.entity.NotificationLog;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import com.vrtechnologies.vrtech.entity.enums.ReviewStatus;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.service.AdminService;
import com.vrtechnologies.vrtech.service.AdminActivityLogService;
import com.vrtechnologies.vrtech.service.BackInStockService;
import com.vrtechnologies.vrtech.service.AdminEngagementService;
import com.vrtechnologies.vrtech.service.CatalogService;
import com.vrtechnologies.vrtech.service.CustomerInsightService;
import com.vrtechnologies.vrtech.service.CloudinaryService;
import com.vrtechnologies.vrtech.service.EnquiryService;
import com.vrtechnologies.vrtech.service.InventoryService;
import com.vrtechnologies.vrtech.service.NotificationService;
import com.vrtechnologies.vrtech.service.OrderService;
import com.vrtechnologies.vrtech.service.PermissionService;
import com.vrtechnologies.vrtech.service.PincodeDeliveryService;
import com.vrtechnologies.vrtech.service.ProductReviewService;
import com.vrtechnologies.vrtech.service.RazorpayService;
import com.vrtechnologies.vrtech.service.ReturnRequestService;
import com.vrtechnologies.vrtech.service.SystemHealthService;
import com.vrtechnologies.vrtech.service.UserContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminActivityLogService activityLogService;
    private final AdminEngagementService adminEngagementService;
    private final OrderService orderService;
    private final EnquiryService enquiryService;
    private final CatalogService catalogService;
    private final CloudinaryService cloudinaryService;
    private final PermissionService permissionService;
    private final PincodeDeliveryService pincodeDeliveryService;
    private final ProductReviewService productReviewService;
    private final UserContextService userContextService;
    private final RazorpayService razorpayService;
    private final ReturnRequestService returnRequestService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final SystemHealthService systemHealthService;
    private final BackInStockService backInStockService;
    private final CustomerInsightService customerInsightService;

    public AdminController(
            AdminService adminService,
            AdminActivityLogService activityLogService,
            AdminEngagementService adminEngagementService,
            OrderService orderService,
            EnquiryService enquiryService,
            CatalogService catalogService,
            CloudinaryService cloudinaryService,
            PermissionService permissionService,
            PincodeDeliveryService pincodeDeliveryService,
            ProductReviewService productReviewService,
            UserContextService userContextService,
            RazorpayService razorpayService,
            ReturnRequestService returnRequestService,
            InventoryService inventoryService,
            NotificationService notificationService,
            SystemHealthService systemHealthService,
            BackInStockService backInStockService,
            CustomerInsightService customerInsightService
    ) {
        this.adminService = adminService;
        this.activityLogService = activityLogService;
        this.adminEngagementService = adminEngagementService;
        this.orderService = orderService;
        this.enquiryService = enquiryService;
        this.catalogService = catalogService;
        this.cloudinaryService = cloudinaryService;
        this.permissionService = permissionService;
        this.pincodeDeliveryService = pincodeDeliveryService;
        this.productReviewService = productReviewService;
        this.userContextService = userContextService;
        this.razorpayService = razorpayService;
        this.returnRequestService = returnRequestService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.systemHealthService = systemHealthService;
        this.backInStockService = backInStockService;
        this.customerInsightService = customerInsightService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsResponse> dashboard(@RequestParam(required = false, defaultValue = "ALL_TIME") String period) {
        User admin = currentAdmin();
        requirePermission(admin, Module.DASHBOARD, PermissionAction.VIEW);
        return ApiResponse.ok("Dashboard stats fetched", adminService.getDashboardStats(admin, period));
    }

    @GetMapping("/activity-summary")
    public ApiResponse<AdminActivitySummaryResponse> activitySummary() {
        User admin = currentAdmin();
        requirePermission(admin, Module.DASHBOARD, PermissionAction.VIEW);
        return ApiResponse.ok("Admin activity summary fetched", adminService.getAdminActivitySummary());
    }

    @GetMapping("/brands")
    public ApiResponse<List<Brand>> adminBrands() {
        requirePermission(currentAdmin(), Module.BRANDS, PermissionAction.VIEW);
        return ApiResponse.ok("Brands fetched", catalogService.getBrands());
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> adminCategories() {
        requirePermission(currentAdmin(), Module.CATEGORIES, PermissionAction.VIEW);
        return ApiResponse.ok("Categories fetched", catalogService.getCategories());
    }

    @GetMapping("/stores")
    public ApiResponse<List<Store>> adminStores() {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.VIEW);
        return ApiResponse.ok("Stores fetched", catalogService.getStores(true, admin));
    }

    @GetMapping(value = "/stores/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportStores() {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.EXPORT);
        StringBuilder csv = new StringBuilder("id,name,city,phone,active\n");
        for (Store store : catalogService.getStores(true, admin)) {
            csv.append(store.getId()).append(',')
                    .append(escapeCsv(store.getName())).append(',')
                    .append(escapeCsv(store.getCity())).append(',')
                    .append(escapeCsv(store.getPhone())).append(',')
                    .append(store.isActive()).append('\n');
        }
        return csv("stores.csv", csv.toString());
    }

    @GetMapping("/stores/performance")
    public ApiResponse<List<AdminStorePerformanceResponse>> storePerformance(@RequestParam(required = false, defaultValue = "ALL_TIME") String period) {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.VIEW);
        return ApiResponse.ok("Store performance fetched", adminService.getStorePerformance(admin, period));
    }

    @GetMapping("/banners")
    public ApiResponse<List<BannerResponse>> adminBanners() {
        requirePermission(currentAdmin(), Module.BANNERS, PermissionAction.VIEW);
        return ApiResponse.ok("Banners fetched", catalogService.getBanners(true));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserSummaryResponse>> users() {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.VIEW);
        return ApiResponse.ok("Users fetched", adminService.getUsers(admin));
    }

    @GetMapping("/customers/{id}/profile")
    public ApiResponse<Customer360Response> customerProfile(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.VIEW);
        return ApiResponse.ok("Customer profile fetched", customerInsightService.getCustomer360(id));
    }

    @GetMapping(value = "/users/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportUsers() {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.EXPORT);
        StringBuilder csv = new StringBuilder("id,name,email,phone,role,active\n");
        for (UserSummaryResponse user : adminService.getUsers(admin)) {
            csv.append(user.getId()).append(',')
                    .append(escapeCsv(user.getName())).append(',')
                    .append(escapeCsv(user.getEmail())).append(',')
                    .append(escapeCsv(user.getPhone())).append(',')
                    .append(user.getRole()).append(',')
                    .append(user.isActive()).append('\n');
        }
        return csv("customers.csv", csv.toString());
    }

    @GetMapping("/cart-items")
    public ApiResponse<List<AdminCartItemResponse>> cartItems() {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.VIEW);
        return ApiResponse.ok("Cart items fetched", adminEngagementService.getCartItems(admin));
    }

    @DeleteMapping("/cart-items/{id}")
    public ApiResponse<Object> deleteCartItem(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.DELETE);
        adminEngagementService.deleteCartItem(admin, id);
        return ApiResponse.ok("Cart item removed", null);
    }

    @PostMapping("/cart-items/{id}/recover")
    public ApiResponse<CartRecoveryResponse> recoverCartItem(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.UPDATE);
        return ApiResponse.ok("Cart recovery queued", adminEngagementService.recoverCartItem(admin, id));
    }

    @GetMapping("/wishlist-items")
    public ApiResponse<List<AdminWishlistItemResponse>> wishlistItems() {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.VIEW);
        return ApiResponse.ok("Wishlist items fetched", adminEngagementService.getWishlistItems(admin));
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ProductReviewResponse>> reviews() {
        User admin = currentAdmin();
        requirePermission(admin, Module.REVIEWS, PermissionAction.VIEW);
        return ApiResponse.ok("Reviews fetched", productReviewService.getReviews(admin));
    }

    @PostMapping("/reviews")
    public ApiResponse<ProductReviewResponse> createReview(@Valid @RequestBody ProductReviewRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.REVIEWS, PermissionAction.CREATE);
        return ApiResponse.ok("Review created", productReviewService.saveReview(admin, request, null));
    }

    @PutMapping("/reviews/{id}")
    public ApiResponse<ProductReviewResponse> updateReview(@PathVariable Long id, @Valid @RequestBody ProductReviewRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.REVIEWS, PermissionAction.UPDATE);
        return ApiResponse.ok("Review updated", productReviewService.saveReview(admin, request, id));
    }

    @PatchMapping("/reviews/{id}/status")
    public ApiResponse<ProductReviewResponse> updateReviewStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.REVIEWS, PermissionAction.APPROVE);
        ReviewStatus status = parseEnum(ReviewStatus.class, request.getValue());
        return ApiResponse.ok("Review status updated", productReviewService.updateStatus(admin, id, status));
    }

    @PatchMapping("/reviews/{id}/featured")
    public ApiResponse<ProductReviewResponse> toggleReviewFeatured(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.REVIEWS, PermissionAction.UPDATE);
        return ApiResponse.ok("Review featured state updated", productReviewService.toggleFeatured(admin, id));
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Object> deleteReview(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.REVIEWS, PermissionAction.DELETE);
        productReviewService.deleteReview(admin, id);
        return ApiResponse.ok("Review deleted", null);
    }

    @DeleteMapping("/wishlist-items/{id}")
    public ApiResponse<Object> deleteWishlistItem(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.DELETE);
        adminEngagementService.deleteWishlistItem(admin, id);
        return ApiResponse.ok("Wishlist item removed", null);
    }

    @GetMapping("/coupons")
    public ApiResponse<List<Coupon>> coupons() {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.VIEW);
        return ApiResponse.ok("Coupons fetched", adminService.getCoupons());
    }

    @GetMapping("/coupons/analytics")
    public ApiResponse<List<CouponAnalyticsResponse>> couponAnalytics() {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.VIEW);
        return ApiResponse.ok("Coupon analytics fetched", adminService.getCouponAnalytics());
    }

    @GetMapping(value = "/coupons/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCoupons() {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.EXPORT);
        StringBuilder csv = new StringBuilder("id,code,discount,minOrder,usageCount,totalDiscountGiven,totalRevenueGenerated,status\n");
        for (Coupon coupon : adminService.getCoupons()) {
            csv.append(coupon.getId()).append(',')
                    .append(escapeCsv(coupon.getCode())).append(',')
                    .append(coupon.getDiscount()).append(',')
                    .append(coupon.getMinOrder()).append(',')
                    .append(coupon.getUsageCount()).append(',')
                    .append(coupon.getTotalDiscountGiven()).append(',')
                    .append(coupon.getTotalRevenueGenerated()).append(',')
                    .append(coupon.getStatus()).append('\n');
        }
        return csv("coupons.csv", csv.toString());
    }

    @PostMapping("/coupons")
    public ApiResponse<Coupon> createCoupon(@Valid @RequestBody CouponRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.COUPONS, PermissionAction.CREATE);
        Coupon coupon = adminService.saveCoupon(request, null);
        activityLogService.log(admin, Module.COUPONS, PermissionAction.CREATE, "Coupon", coupon.getId(), "Coupon created: " + coupon.getCode());
        return ApiResponse.ok("Coupon created", coupon);
    }

    @PutMapping("/coupons/{id}")
    public ApiResponse<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.COUPONS, PermissionAction.UPDATE);
        Coupon coupon = adminService.saveCoupon(request, id);
        activityLogService.log(admin, Module.COUPONS, PermissionAction.UPDATE, "Coupon", id, "Coupon updated: " + coupon.getCode());
        return ApiResponse.ok("Coupon updated", coupon);
    }

    @DeleteMapping("/coupons/{id}")
    public ApiResponse<Object> deleteCoupon(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.COUPONS, PermissionAction.DELETE);
        adminService.deleteCoupon(id);
        activityLogService.log(admin, Module.COUPONS, PermissionAction.DELETE, "Coupon", id, "Coupon deleted");
        return ApiResponse.ok("Coupon deleted", null);
    }

    @GetMapping("/delivery/pincodes")
    public ApiResponse<List<PincodeDeliveryRuleResponse>> deliveryPincodes() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Delivery pincode rules fetched", pincodeDeliveryService.getRules());
    }

    @GetMapping(value = "/delivery/pincodes/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportDeliveryPincodes() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.EXPORT);
        return csv("delivery-pincodes.csv", pincodeDeliveryService.exportCsv());
    }

    @PostMapping("/delivery/pincodes")
    public ApiResponse<PincodeDeliveryRuleResponse> createDeliveryPincode(@Valid @RequestBody PincodeDeliveryRuleRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.CREATE);
        PincodeDeliveryRuleResponse response = pincodeDeliveryService.saveRule(request, null);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.CREATE, "PincodeDeliveryRule", response.getId(), "Delivery pincode created: " + response.getPincode());
        return ApiResponse.ok("Delivery pincode created", response);
    }

    @PutMapping("/delivery/pincodes/{id}")
    public ApiResponse<PincodeDeliveryRuleResponse> updateDeliveryPincode(@PathVariable Long id, @Valid @RequestBody PincodeDeliveryRuleRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.UPDATE);
        PincodeDeliveryRuleResponse response = pincodeDeliveryService.saveRule(request, id);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.UPDATE, "PincodeDeliveryRule", id, "Delivery pincode updated: " + response.getPincode());
        return ApiResponse.ok("Delivery pincode updated", response);
    }

    @DeleteMapping("/delivery/pincodes/{id}")
    public ApiResponse<Object> deleteDeliveryPincode(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.DELETE);
        pincodeDeliveryService.deleteRule(id);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.DELETE, "PincodeDeliveryRule", id, "Delivery pincode deleted");
        return ApiResponse.ok("Delivery pincode deleted", null);
    }

    @PostMapping(value = "/delivery/pincodes/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductImportResponse> importDeliveryPincodes(@RequestParam("file") MultipartFile file) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.CREATE);
        ProductImportResponse response = pincodeDeliveryService.importCsv(file);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.CREATE, "PincodeDeliveryRule", null, "Delivery pincode CSV imported");
        return ApiResponse.ok("Delivery pincode CSV processed", response);
    }

    // --- ZONES MANAGEMENT ---
    @GetMapping("/delivery/zones")
    public ApiResponse<List<PincodeZone>> deliveryZones() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Delivery zones fetched", pincodeDeliveryService.getZones());
    }

    @PostMapping("/delivery/zones")
    public ApiResponse<PincodeZone> createDeliveryZone(@Valid @RequestBody PincodeZone zone) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.CREATE);
        PincodeZone response = pincodeDeliveryService.saveZone(zone);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.CREATE, "PincodeZone", response.getId(), "Delivery zone created: " + response.getZoneName());
        return ApiResponse.ok("Delivery zone created", response);
    }

    @PutMapping("/delivery/zones/{id}")
    public ApiResponse<PincodeZone> updateDeliveryZone(@PathVariable Long id, @Valid @RequestBody PincodeZone zone) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.UPDATE);
        zone.setId(id);
        PincodeZone response = pincodeDeliveryService.saveZone(zone);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.UPDATE, "PincodeZone", id, "Delivery zone updated: " + response.getZoneName());
        return ApiResponse.ok("Delivery zone updated", response);
    }

    @DeleteMapping("/delivery/zones/{id}")
    public ApiResponse<Object> deleteDeliveryZone(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.DELETE);
        pincodeDeliveryService.deleteZone(id);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.DELETE, "PincodeZone", id, "Delivery zone deleted");
        return ApiResponse.ok("Delivery zone deleted", null);
    }

    // --- BLACKLIST MANAGEMENT ---
    @GetMapping("/delivery/blacklist")
    public ApiResponse<List<PincodeBlacklist>> deliveryBlacklist() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Delivery blacklist fetched", pincodeDeliveryService.getBlacklist());
    }

    @PostMapping("/delivery/blacklist")
    public ApiResponse<PincodeBlacklist> createBlacklistEntry(@Valid @RequestBody PincodeBlacklist entry) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.CREATE);
        PincodeBlacklist response = pincodeDeliveryService.saveBlacklist(entry);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.CREATE, "PincodeBlacklist", response.getId(), "Pincode blacklisted: " + response.getPincode());
        return ApiResponse.ok("Pincode blacklisted", response);
    }

    @PutMapping("/delivery/blacklist/{id}")
    public ApiResponse<PincodeBlacklist> updateBlacklistEntry(@PathVariable Long id, @Valid @RequestBody PincodeBlacklist entry) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.UPDATE);
        entry.setId(id);
        PincodeBlacklist response = pincodeDeliveryService.saveBlacklist(entry);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.UPDATE, "PincodeBlacklist", id, "Pincode blacklist updated: " + response.getPincode());
        return ApiResponse.ok("Blacklist entry updated", response);
    }

    @DeleteMapping("/delivery/blacklist/{id}")
    public ApiResponse<Object> deleteBlacklistEntry(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.DELETE);
        pincodeDeliveryService.deleteBlacklist(id);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.DELETE, "PincodeBlacklist", id, "Pincode removed from blacklist");
        return ApiResponse.ok("Pincode removed from blacklist", null);
    }

    // --- HOLIDAYS MANAGEMENT ---
    @GetMapping("/delivery/holidays")
    public ApiResponse<List<Holiday>> deliveryHolidays() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Holidays fetched", pincodeDeliveryService.getHolidays());
    }

    @PostMapping("/delivery/holidays")
    public ApiResponse<Holiday> createHoliday(@Valid @RequestBody Holiday holiday) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.CREATE);
        Holiday response = pincodeDeliveryService.saveHoliday(holiday);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.CREATE, "Holiday", response.getId(), "Holiday added: " + response.getName());
        return ApiResponse.ok("Holiday added", response);
    }

    @PutMapping("/delivery/holidays/{id}")
    public ApiResponse<Holiday> updateHoliday(@PathVariable Long id, @Valid @RequestBody Holiday holiday) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.UPDATE);
        holiday.setId(id);
        Holiday response = pincodeDeliveryService.saveHoliday(holiday);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.UPDATE, "Holiday", id, "Holiday updated: " + response.getName());
        return ApiResponse.ok("Holiday updated", response);
    }

    @DeleteMapping("/delivery/holidays/{id}")
    public ApiResponse<Object> deleteHoliday(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.DELETE);
        pincodeDeliveryService.deleteHoliday(id);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.DELETE, "Holiday", id, "Holiday deleted");
        return ApiResponse.ok("Holiday deleted", null);
    }

    // --- LOOKUP LOGS ---
    @GetMapping("/delivery/logs")
    public ApiResponse<List<PincodeLookupLog>> deliveryLookupLogs() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Delivery lookup logs fetched", pincodeDeliveryService.getLookupLogs());
    }

    @GetMapping("/settings")
    public ApiResponse<SiteSettings> settings() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Settings fetched", adminService.getSiteSettings());
    }

    @GetMapping(value = "/settings/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportSettings() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.EXPORT);
        SiteSettings settings = adminService.getSiteSettings();
        StringBuilder csv = new StringBuilder("key,value\n");
        csv.append("companyName,").append(escapeCsv(settings.getCompanyName())).append('\n');
        csv.append("supportEmail,").append(escapeCsv(settings.getSupportEmail())).append('\n');
        csv.append("supportPhone,").append(escapeCsv(settings.getSupportPhone())).append('\n');
        csv.append("gstEnabled,").append(settings.isGstEnabled()).append('\n');
        csv.append("gstRate,").append(settings.getGstRate()).append('\n');
        csv.append("gstNumber,").append(escapeCsv(settings.getGstNumber())).append('\n');
        return csv("settings.csv", csv.toString());
    }

    @PutMapping("/settings")
    public ApiResponse<SiteSettings> updateSettings(@Valid @RequestBody SiteSettingsRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.UPDATE);
        SiteSettings settings = adminService.saveSiteSettings(request);
        activityLogService.log(admin, Module.SETTINGS, PermissionAction.UPDATE, "SiteSettings", settings.getId(), "Site settings updated");
        return ApiResponse.ok("Settings updated", settings);
    }

    @GetMapping("/payments/razorpay")
    public ApiResponse<RazorpaySettingsResponse> razorpaySettings() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Razorpay settings fetched", razorpayService.settings());
    }

    @GetMapping("/system/health")
    public ApiResponse<SystemHealthResponse> systemHealth() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("System health fetched", systemHealthService.currentHealth());
    }

    @GetMapping("/system-health")
    public ApiResponse<SystemHealthResponse> systemHealthAlias() {
        return systemHealth();
    }

    @PatchMapping("/users/{id}/toggle")
    public ApiResponse<UserSummaryResponse> toggleUser(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.UPDATE);
        return ApiResponse.ok("User updated", adminService.toggleUser(admin, id));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> orders(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Orders fetched", orderService.getAllOrders(admin, parseStart(startDate), parseEnd(endDate)));
    }

    @GetMapping("/payments/failed")
    public ApiResponse<List<OrderResponse>> failedPayments(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Failed payments fetched", orderService.getFailedPaymentOrders(admin, parseStart(startDate), parseEnd(endDate)));
    }

    @GetMapping("/payments/webhook-events")
    public ApiResponse<List<PaymentWebhookEventResponse>> paymentWebhookEvents() {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Payment webhook events fetched", orderService.getPaymentWebhookEvents(admin));
    }

    @PostMapping("/payments/failed/{orderId}/recover")
    public ApiResponse<PaymentRecoveryResponse> recoverFailedPayment(@PathVariable Long orderId) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        return ApiResponse.ok("Payment recovery queued", orderService.recoverFailedPayment(admin, orderId));
    }

    @GetMapping(value = "/orders/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportOrders() {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.EXPORT);
        StringBuilder csv = new StringBuilder("id,orderNumber,customer,phone,amount,status,paymentStatus,createdAt\n");
        for (OrderResponse order : orderService.getAllOrders(admin)) {
            csv.append(order.getId()).append(',')
                    .append(escapeCsv(order.getOrderNumber())).append(',')
                    .append(escapeCsv(order.getContactName())).append(',')
                    .append(escapeCsv(order.getContactPhone())).append(',')
                    .append(order.getTotalAmount()).append(',')
                    .append(order.getStatus()).append(',')
                    .append(order.getPaymentStatus()).append(',')
                    .append(order.getCreatedAt()).append('\n');
        }
        return csv("orders.csv", csv.toString());
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponse> order(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Order fetched", orderService.getAdminOrder(admin, id));
    }

    @GetMapping("/orders/{id}/timeline")
    public ApiResponse<List<OrderTimelineEventResponse>> orderTimeline(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Order timeline fetched", orderService.getAdminOrder(admin, id).getTimeline());
    }

    @GetMapping("/orders/{id}/refunds")
    public ApiResponse<List<RefundTransactionResponse>> orderRefunds(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Refund transactions fetched", orderService.getRefundTransactions(admin, id));
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        OrderStatus status = parseEnum(OrderStatus.class, request.getValue());
        return ApiResponse.ok("Order status updated", orderService.updateOrderStatus(admin, id, status));
    }

    @PatchMapping("/orders/{id}/payment-status")
    public ApiResponse<OrderResponse> updatePaymentStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        PaymentStatus paymentStatus = parseEnum(PaymentStatus.class, request.getValue());
        return ApiResponse.ok("Payment status updated", orderService.updatePaymentStatus(admin, id, paymentStatus));
    }

    @PatchMapping("/orders/{id}/shipment")
    public ApiResponse<OrderResponse> updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentUpdateRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        return ApiResponse.ok("Shipment updated", orderService.updateShipment(admin, id, request));
    }

    @GetMapping(value = "/orders/{id}/invoice")
    public ResponseEntity<byte[]> orderInvoice(@PathVariable Long id, @RequestParam(required = false) String format) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        OrderResponse order = orderService.getAdminOrder(admin, id);
        if ("WORD".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + order.getInvoiceNumber() + ".doc\"")
                    .contentType(MediaType.parseMediaType("application/msword"))
                    .body(orderService.generateInvoiceWordForAdmin(admin, id));
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + order.getInvoiceNumber() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(orderService.generateInvoicePdfForAdmin(admin, id));
        }
    }

    @GetMapping("/returns")
    public ApiResponse<List<ReturnRequestResponse>> returns(@RequestParam(required = false) ReturnRequestStatus status) {
        requirePermission(currentAdmin(), Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Returns fetched", returnRequestService.list(status));
    }

    @PatchMapping("/returns/{id}/approve")
    public ApiResponse<ReturnRequestResponse> approveReturn(@PathVariable Long id, @RequestBody(required = false) ReturnDecisionRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.APPROVE);
        ReturnRequestResponse response = returnRequestService.approve(admin, id, request != null ? request.getNote() : null);
        activityLogService.log(admin, Module.ORDERS, PermissionAction.APPROVE, "ReturnRequest", id, "Return approved");
        return ApiResponse.ok("Return approved", response);
    }

    @PatchMapping("/returns/{id}/reject")
    public ApiResponse<ReturnRequestResponse> rejectReturn(@PathVariable Long id, @RequestBody(required = false) ReturnDecisionRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.APPROVE);
        ReturnRequestResponse response = returnRequestService.reject(admin, id, request != null ? request.getNote() : null);
        activityLogService.log(admin, Module.ORDERS, PermissionAction.APPROVE, "ReturnRequest", id, "Return rejected");
        return ApiResponse.ok("Return rejected", response);
    }

    @PatchMapping("/returns/{id}/refund")
    public ApiResponse<ReturnRequestResponse> refundReturn(@PathVariable Long id, @RequestBody(required = false) ReturnDecisionRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        ReturnRequestResponse response = returnRequestService.markRefunded(admin, id, request != null ? request.getNote() : null);
        activityLogService.log(admin, Module.ORDERS, PermissionAction.UPDATE, "ReturnRequest", id, "Return refunded");
        return ApiResponse.ok("Return refunded", response);
    }

    @PatchMapping("/returns/{id}/pickup")
    public ApiResponse<ReturnRequestResponse> scheduleReturnPickup(@PathVariable Long id, @RequestBody(required = false) ReturnPickupRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        ReturnRequestResponse response = returnRequestService.schedulePickup(admin, id, request);
        activityLogService.log(admin, Module.ORDERS, PermissionAction.UPDATE, "ReturnRequest", id, "Return pickup scheduled");
        return ApiResponse.ok("Return pickup scheduled", response);
    }

    @PatchMapping("/returns/{id}/picked-up")
    public ApiResponse<ReturnRequestResponse> markReturnPickedUp(@PathVariable Long id, @RequestBody(required = false) ReturnDecisionRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        ReturnRequestResponse response = returnRequestService.markPickedUp(admin, id, request != null ? request.getNote() : null);
        activityLogService.log(admin, Module.ORDERS, PermissionAction.UPDATE, "ReturnRequest", id, "Return picked up");
        return ApiResponse.ok("Return picked up", response);
    }

    @PatchMapping("/returns/{id}/inspect")
    public ApiResponse<ReturnRequestResponse> inspectReturn(@PathVariable Long id, @RequestBody(required = false) ReturnDecisionRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.UPDATE);
        ReturnRequestResponse response = returnRequestService.inspect(admin, id, request != null ? request.getNote() : null);
        activityLogService.log(admin, Module.ORDERS, PermissionAction.UPDATE, "ReturnRequest", id, "Return inspected");
        return ApiResponse.ok("Return inspected", response);
    }

    @GetMapping("/inventory/movements")
    public ApiResponse<List<StockMovementResponse>> stockMovements() {
        requirePermission(currentAdmin(), Module.INVENTORY, PermissionAction.VIEW);
        return ApiResponse.ok("Stock movements fetched", inventoryService.latest());
    }

    @GetMapping(value = "/inventory/movements/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportStockMovements() {
        requirePermission(currentAdmin(), Module.INVENTORY, PermissionAction.EXPORT);
        StringBuilder csv = new StringBuilder("id,product,type,quantity,previousStock,newStock,reason,createdAt\n");
        for (StockMovementResponse movement : inventoryService.latest()) {
            csv.append(movement.getId()).append(',')
                    .append(escapeCsv(movement.getProductTitle())).append(',')
                    .append(movement.getMovementType()).append(',')
                    .append(movement.getQuantity()).append(',')
                    .append(movement.getPreviousStock()).append(',')
                    .append(movement.getNewStock()).append(',')
                    .append(escapeCsv(movement.getReason())).append(',')
                    .append(movement.getCreatedAt()).append('\n');
        }
        return csv("inventory-movements.csv", csv.toString());
    }

    @PostMapping("/inventory/adjust")
    public ApiResponse<StockMovementResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.INVENTORY, PermissionAction.UPDATE);
        return ApiResponse.ok("Stock adjusted", inventoryService.adjust(admin, request));
    }

    @GetMapping("/inventory/transfers")
    public ApiResponse<List<StockTransferResponse>> stockTransfers() {
        requirePermission(currentAdmin(), Module.INVENTORY, PermissionAction.VIEW);
        return ApiResponse.ok("Stock transfers fetched", inventoryService.latestTransfers());
    }

    @PostMapping("/inventory/transfers")
    public ApiResponse<StockTransferResponse> transferStock(@Valid @RequestBody StockTransferRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.INVENTORY, PermissionAction.UPDATE);
        return ApiResponse.ok("Stock transfer completed", inventoryService.transfer(admin, request));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<NotificationLog>> notifications() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Notifications fetched", notificationService.latest());
    }

    @GetMapping("/notifications/unread")
    public ApiResponse<List<NotificationLog>> unreadNotifications() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        List<NotificationLog> unread = notificationService.latest().stream()
                .filter(log -> !log.isRead())
                .toList();
        return ApiResponse.ok("Unread notifications fetched", unread);
    }

    @GetMapping("/export-center")
    public ApiResponse<ExportManifestResponse> exportCenter() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Export center fetched", ExportManifestResponse.builder()
                .exports(List.of(
                        "/api/admin/products/export",
                        "/api/admin/orders/export",
                        "/api/admin/users/export",
                        "/api/admin/inventory/movements/export",
                        "/api/admin/settings/export",
                        "/api/admin/coupons/export",
                        "/api/admin/stores/export"
                ))
                .build());
    }

    @GetMapping(value = "/export-center/backup.zip", produces = "application/zip")
    public ResponseEntity<byte[]> exportBackupZip() {
        User admin = currentAdmin();
        requirePermission(admin, Module.SETTINGS, PermissionAction.EXPORT);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                zipCsv(zip, "customers.csv", usersCsv(admin));
                zipCsv(zip, "orders.csv", ordersCsv(admin));
                zipCsv(zip, "stores.csv", storesCsv(admin));
                zipCsv(zip, "coupons.csv", couponsCsv());
                zipCsv(zip, "settings.csv", settingsCsv());
                zipCsv(zip, "inventory-movements.csv", stockMovementsCsv());
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vrtech-backup.zip\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(bytes.toByteArray());
        } catch (Exception exception) {
            throw new BadRequestException("Unable to create backup zip");
        }
    }

    @PatchMapping("/notifications/{id}/read")
    public ApiResponse<NotificationLog> markNotificationRead(@PathVariable Long id) {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Notification marked read", notificationService.markRead(id));
    }

    @PatchMapping("/notifications/read-all")
    public ApiResponse<Object> markAllNotificationsRead() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        notificationService.markAllRead();
        return ApiResponse.ok("Notifications marked read", null);
    }

    @GetMapping("/back-in-stock-requests")
    public ApiResponse<List<BackInStockRequestResponse>> backInStockRequests() {
        requirePermission(currentAdmin(), Module.INVENTORY, PermissionAction.VIEW);
        return ApiResponse.ok("Back-in-stock requests fetched", backInStockService.latest());
    }

    @PatchMapping("/back-in-stock-requests/{id}/status")
    public ApiResponse<BackInStockRequestResponse> updateBackInStockRequestStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        requirePermission(currentAdmin(), Module.INVENTORY, PermissionAction.UPDATE);
        return ApiResponse.ok("Back-in-stock request updated", backInStockService.updateStatus(id, request.getValue()));
    }

    @DeleteMapping("/back-in-stock-requests/{id}")
    public ApiResponse<Object> deleteBackInStockRequest(@PathVariable Long id) {
        requirePermission(currentAdmin(), Module.INVENTORY, PermissionAction.DELETE);
        backInStockService.delete(id);
        return ApiResponse.ok("Back-in-stock request deleted", null);
    }

    @GetMapping("/enquiries")
    public ApiResponse<List<Enquiry>> enquiries() {
        User admin = currentAdmin();
        requirePermission(admin, Module.ENQUIRIES, PermissionAction.VIEW);
        return ApiResponse.ok("Enquiries fetched", enquiryService.getAll(admin));
    }

    @PatchMapping("/enquiries/{id}/status")
    public ApiResponse<Enquiry> updateEnquiryStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ENQUIRIES, PermissionAction.UPDATE);
        EnquiryStatus status = parseEnum(EnquiryStatus.class, request.getValue());
        Enquiry enquiry = enquiryService.updateStatus(admin, id, status);
        activityLogService.log(admin, Module.ENQUIRIES, PermissionAction.UPDATE, "Enquiry", id, "Enquiry status updated to " + status);
        return ApiResponse.ok("Enquiry updated", enquiry);
    }

    @PostMapping("/stores")
    public ApiResponse<Store> createStore(@Valid @RequestBody StoreRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.CREATE);
        Store store = catalogService.saveStore(admin, request, null);
        activityLogService.log(admin, Module.STORES, PermissionAction.CREATE, "Store", store.getId(), "Store created: " + store.getName());
        return ApiResponse.ok("Store created", store);
    }

    @PutMapping("/stores/{id}")
    public ApiResponse<Store> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.UPDATE);
        Store store = catalogService.saveStore(admin, request, id);
        activityLogService.log(admin, Module.STORES, PermissionAction.UPDATE, "Store", id, "Store updated: " + store.getName());
        return ApiResponse.ok("Store updated", store);
    }

    @DeleteMapping("/stores/{id}")
    public ApiResponse<Object> deleteStore(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.DELETE);
        catalogService.deleteStore(admin, id);
        activityLogService.log(admin, Module.STORES, PermissionAction.DELETE, "Store", id, "Store deleted");
        return ApiResponse.ok("Store deleted", null);
    }

    @PostMapping("/banners")
    public ApiResponse<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.BANNERS, PermissionAction.CREATE);
        BannerResponse banner = catalogService.saveBanner(request, null);
        activityLogService.log(admin, Module.BANNERS, PermissionAction.CREATE, "Banner", banner.getId(), "Banner created");
        return ApiResponse.ok("Banner created", banner);
    }

    @PutMapping("/banners/{id}")
    public ApiResponse<BannerResponse> updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.BANNERS, PermissionAction.UPDATE);
        BannerResponse banner = catalogService.saveBanner(request, id);
        activityLogService.log(admin, Module.BANNERS, PermissionAction.UPDATE, "Banner", id, "Banner updated");
        return ApiResponse.ok("Banner updated", banner);
    }

    @DeleteMapping("/banners/{id}")
    public ApiResponse<Object> deleteBanner(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.BANNERS, PermissionAction.DELETE);
        catalogService.deleteBanner(id);
        activityLogService.log(admin, Module.BANNERS, PermissionAction.DELETE, "Banner", id, "Banner deleted");
        return ApiResponse.ok("Banner deleted", null);
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CATEGORIES, PermissionAction.CREATE);
        Category category = catalogService.createCategory(request);
        activityLogService.log(admin, Module.CATEGORIES, PermissionAction.CREATE, "Category", category.getId(), "Category created: " + category.getName());
        return ApiResponse.ok("Category created", category);
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CATEGORIES, PermissionAction.UPDATE);
        Category category = catalogService.saveCategory(request, id);
        activityLogService.log(admin, Module.CATEGORIES, PermissionAction.UPDATE, "Category", id, "Category updated: " + category.getName());
        return ApiResponse.ok("Category updated", category);
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Object> deleteCategory(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CATEGORIES, PermissionAction.DELETE);
        catalogService.deleteCategory(id);
        activityLogService.log(admin, Module.CATEGORIES, PermissionAction.DELETE, "Category", id, "Category deleted");
        return ApiResponse.ok("Category deleted", null);
    }

    @PostMapping("/brands")
    public ApiResponse<Brand> createBrand(@Valid @RequestBody BrandRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.BRANDS, PermissionAction.CREATE);
        Brand brand = catalogService.createBrand(request);
        activityLogService.log(admin, Module.BRANDS, PermissionAction.CREATE, "Brand", brand.getId(), "Brand created: " + brand.getName());
        return ApiResponse.ok("Brand created", brand);
    }

    @PutMapping("/brands/{id}")
    public ApiResponse<Brand> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.BRANDS, PermissionAction.UPDATE);
        Brand brand = catalogService.saveBrand(request, id);
        activityLogService.log(admin, Module.BRANDS, PermissionAction.UPDATE, "Brand", id, "Brand updated: " + brand.getName());
        return ApiResponse.ok("Brand updated", brand);
    }

    @DeleteMapping("/brands/{id}")
    public ApiResponse<Object> deleteBrand(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.BRANDS, PermissionAction.DELETE);
        catalogService.deleteBrand(id);
        activityLogService.log(admin, Module.BRANDS, PermissionAction.DELETE, "Brand", id, "Brand deleted");
        return ApiResponse.ok("Brand deleted", null);
    }

    @PostMapping("/media/upload")
    public ApiResponse<MediaUploadResponse> uploadMedia(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "general") String folder) {
        requireAnyMediaPermission(currentAdmin());
        String contentType = file != null ? file.getContentType() : null;
        if (contentType == null) {
            throw new BadRequestException("File type could not be detected");
        }
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedType.startsWith("image/")) {
            return ApiResponse.ok("Media uploaded", cloudinaryService.uploadImage(file, folder));
        }
        if (normalizedType.startsWith("video/")) {
            return ApiResponse.ok("Media uploaded", cloudinaryService.uploadMedia(file, folder));
        }
        throw new BadRequestException("Only image or video files are allowed");
    }

    @DeleteMapping("/media")
    public ApiResponse<Object> deleteMedia(@RequestParam String publicId) {
        requireAnyMediaPermission(currentAdmin());
        cloudinaryService.deleteAsset(publicId);
        return ApiResponse.ok("Media deleted", null);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String rawValue) {
        return Enum.valueOf(enumType, rawValue.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    }

    private ResponseEntity<byte[]> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }

    private void zipCsv(ZipOutputStream zip, String filename, String body) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(filename));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String usersCsv(User admin) {
        StringBuilder csv = new StringBuilder("id,name,email,phone,role,active\n");
        for (UserSummaryResponse user : adminService.getUsers(admin)) {
            csv.append(user.getId()).append(',').append(escapeCsv(user.getName())).append(',').append(escapeCsv(user.getEmail())).append(',')
                    .append(escapeCsv(user.getPhone())).append(',').append(user.getRole()).append(',').append(user.isActive()).append('\n');
        }
        return csv.toString();
    }

    private String ordersCsv(User admin) {
        StringBuilder csv = new StringBuilder("id,orderNumber,customer,phone,amount,status,paymentStatus,createdAt\n");
        for (OrderResponse order : orderService.getAllOrders(admin)) {
            csv.append(order.getId()).append(',').append(escapeCsv(order.getOrderNumber())).append(',').append(escapeCsv(order.getContactName())).append(',')
                    .append(escapeCsv(order.getContactPhone())).append(',').append(order.getTotalAmount()).append(',').append(order.getStatus()).append(',')
                    .append(order.getPaymentStatus()).append(',').append(order.getCreatedAt()).append('\n');
        }
        return csv.toString();
    }

    private String storesCsv(User admin) {
        StringBuilder csv = new StringBuilder("id,name,city,phone,active\n");
        for (Store store : catalogService.getStores(true, admin)) {
            csv.append(store.getId()).append(',').append(escapeCsv(store.getName())).append(',').append(escapeCsv(store.getCity())).append(',')
                    .append(escapeCsv(store.getPhone())).append(',').append(store.isActive()).append('\n');
        }
        return csv.toString();
    }

    private String couponsCsv() {
        StringBuilder csv = new StringBuilder("id,code,discount,minOrder,usageCount,totalDiscountGiven,totalRevenueGenerated,status\n");
        for (Coupon coupon : adminService.getCoupons()) {
            csv.append(coupon.getId()).append(',').append(escapeCsv(coupon.getCode())).append(',').append(coupon.getDiscount()).append(',')
                    .append(coupon.getMinOrder()).append(',').append(coupon.getUsageCount()).append(',').append(coupon.getTotalDiscountGiven()).append(',')
                    .append(coupon.getTotalRevenueGenerated()).append(',').append(coupon.getStatus()).append('\n');
        }
        return csv.toString();
    }

    private String settingsCsv() {
        SiteSettings settings = adminService.getSiteSettings();
        return "key,value\n"
                + "companyName," + escapeCsv(settings.getCompanyName()) + "\n"
                + "supportEmail," + escapeCsv(settings.getSupportEmail()) + "\n"
                + "supportPhone," + escapeCsv(settings.getSupportPhone()) + "\n"
                + "gstEnabled," + settings.isGstEnabled() + "\n"
                + "gstRate," + settings.getGstRate() + "\n"
                + "gstNumber," + escapeCsv(settings.getGstNumber()) + "\n";
    }

    private String stockMovementsCsv() {
        StringBuilder csv = new StringBuilder("id,product,type,quantity,previousStock,newStock,reason,createdAt\n");
        for (StockMovementResponse movement : inventoryService.latest()) {
            csv.append(movement.getId()).append(',').append(escapeCsv(movement.getProductTitle())).append(',').append(movement.getMovementType()).append(',')
                    .append(movement.getQuantity()).append(',').append(movement.getPreviousStock()).append(',').append(movement.getNewStock()).append(',')
                    .append(escapeCsv(movement.getReason())).append(',').append(movement.getCreatedAt()).append('\n');
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private LocalDateTime parseStart(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseEnd(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value).plusDays(1).atStartOfDay();
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }

    private void requirePermission(User admin, Module module, PermissionAction action) {
        permissionService.requirePermission(admin, module, action);
    }

    private void requireAnyMediaPermission(User admin) {
        boolean allowed = permissionService.hasPermission(admin, Module.PRODUCTS, PermissionAction.UPDATE)
                || permissionService.hasPermission(admin, Module.BRANDS, PermissionAction.UPDATE)
                || permissionService.hasPermission(admin, Module.CATEGORIES, PermissionAction.UPDATE)
                || permissionService.hasPermission(admin, Module.STORES, PermissionAction.UPDATE)
                || permissionService.hasPermission(admin, Module.BANNERS, PermissionAction.UPDATE)
                || permissionService.hasPermission(admin, Module.WEBSITE_CONTENT, PermissionAction.UPDATE);
        if (!allowed) {
            throw new AccessDeniedException("Missing permission to upload or delete media");
        }
    }
}
