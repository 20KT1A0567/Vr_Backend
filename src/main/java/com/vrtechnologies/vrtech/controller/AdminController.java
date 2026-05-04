package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.BannerRequest;
import com.vrtechnologies.vrtech.dto.request.BrandRequest;
import com.vrtechnologies.vrtech.dto.request.CategoryRequest;
import com.vrtechnologies.vrtech.dto.request.CouponRequest;
import com.vrtechnologies.vrtech.dto.request.OrderActionRequest;
import com.vrtechnologies.vrtech.dto.request.SiteSettingsRequest;
import com.vrtechnologies.vrtech.dto.request.StatusUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.StoreRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.BannerResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardStatsResponse;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.UserSummaryResponse;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Coupon;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.service.AdminService;
import com.vrtechnologies.vrtech.service.CatalogService;
import com.vrtechnologies.vrtech.service.CloudinaryService;
import com.vrtechnologies.vrtech.service.EnquiryService;
import com.vrtechnologies.vrtech.service.OrderService;
import com.vrtechnologies.vrtech.service.PermissionService;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final EnquiryService enquiryService;
    private final CatalogService catalogService;
    private final CloudinaryService cloudinaryService;
    private final PermissionService permissionService;
    private final UserContextService userContextService;

    public AdminController(
            AdminService adminService,
            OrderService orderService,
            EnquiryService enquiryService,
            CatalogService catalogService,
            CloudinaryService cloudinaryService,
            PermissionService permissionService,
            UserContextService userContextService
    ) {
        this.adminService = adminService;
        this.orderService = orderService;
        this.enquiryService = enquiryService;
        this.catalogService = catalogService;
        this.cloudinaryService = cloudinaryService;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsResponse> dashboard() {
        User admin = currentAdmin();
        requirePermission(admin, Module.DASHBOARD, PermissionAction.VIEW);
        return ApiResponse.ok("Dashboard stats fetched", adminService.getDashboardStats(admin));
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

    @GetMapping("/coupons")
    public ApiResponse<List<Coupon>> coupons() {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.VIEW);
        return ApiResponse.ok("Coupons fetched", adminService.getCoupons());
    }

    @PostMapping("/coupons")
    public ApiResponse<Coupon> createCoupon(@Valid @RequestBody CouponRequest request) {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.CREATE);
        return ApiResponse.ok("Coupon created", adminService.saveCoupon(request, null));
    }

    @PutMapping("/coupons/{id}")
    public ApiResponse<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.UPDATE);
        return ApiResponse.ok("Coupon updated", adminService.saveCoupon(request, id));
    }

    @DeleteMapping("/coupons/{id}")
    public ApiResponse<Object> deleteCoupon(@PathVariable Long id) {
        requirePermission(currentAdmin(), Module.COUPONS, PermissionAction.DELETE);
        adminService.deleteCoupon(id);
        return ApiResponse.ok("Coupon deleted", null);
    }

    @GetMapping("/settings")
    public ApiResponse<SiteSettings> settings() {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.VIEW);
        return ApiResponse.ok("Settings fetched", adminService.getSiteSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<SiteSettings> updateSettings(@Valid @RequestBody SiteSettingsRequest request) {
        requirePermission(currentAdmin(), Module.SETTINGS, PermissionAction.UPDATE);
        return ApiResponse.ok("Settings updated", adminService.saveSiteSettings(request));
    }

    @PatchMapping("/users/{id}/toggle")
    public ApiResponse<UserSummaryResponse> toggleUser(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.CUSTOMERS, PermissionAction.UPDATE);
        return ApiResponse.ok("User updated", adminService.toggleUser(admin, id));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> orders() {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Orders fetched", orderService.getAllOrders(admin));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponse> order(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return ApiResponse.ok("Order fetched", orderService.getAdminOrder(admin, id));
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

    @GetMapping(value = "/orders/{id}/invoice", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> orderInvoice(@PathVariable Long id) {
        User admin = currentAdmin();
        requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        OrderResponse order = orderService.getAdminOrder(admin, id);
        String html = orderService.generateInvoiceHtmlForAdmin(admin, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + order.getInvoiceNumber() + ".html\"")
                .contentType(MediaType.TEXT_HTML)
                .body(html);
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
        return ApiResponse.ok("Enquiry updated", enquiryService.updateStatus(admin, id, status));
    }

    @PostMapping("/stores")
    public ApiResponse<Store> createStore(@Valid @RequestBody StoreRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.CREATE);
        return ApiResponse.ok("Store created", catalogService.saveStore(admin, request, null));
    }

    @PutMapping("/stores/{id}")
    public ApiResponse<Store> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        User admin = currentAdmin();
        requirePermission(admin, Module.STORES, PermissionAction.UPDATE);
        return ApiResponse.ok("Store updated", catalogService.saveStore(admin, request, id));
    }

    @PostMapping("/banners")
    public ApiResponse<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        requirePermission(currentAdmin(), Module.BANNERS, PermissionAction.CREATE);
        return ApiResponse.ok("Banner created", catalogService.saveBanner(request, null));
    }

    @PutMapping("/banners/{id}")
    public ApiResponse<BannerResponse> updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        requirePermission(currentAdmin(), Module.BANNERS, PermissionAction.UPDATE);
        return ApiResponse.ok("Banner updated", catalogService.saveBanner(request, id));
    }

    @DeleteMapping("/banners/{id}")
    public ApiResponse<Object> deleteBanner(@PathVariable Long id) {
        requirePermission(currentAdmin(), Module.BANNERS, PermissionAction.DELETE);
        catalogService.deleteBanner(id);
        return ApiResponse.ok("Banner deleted", null);
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        requirePermission(currentAdmin(), Module.CATEGORIES, PermissionAction.CREATE);
        return ApiResponse.ok("Category created", catalogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        requirePermission(currentAdmin(), Module.CATEGORIES, PermissionAction.UPDATE);
        return ApiResponse.ok("Category updated", catalogService.saveCategory(request, id));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Object> deleteCategory(@PathVariable Long id) {
        requirePermission(currentAdmin(), Module.CATEGORIES, PermissionAction.DELETE);
        catalogService.deleteCategory(id);
        return ApiResponse.ok("Category deleted", null);
    }

    @PostMapping("/brands")
    public ApiResponse<Brand> createBrand(@Valid @RequestBody BrandRequest request) {
        requirePermission(currentAdmin(), Module.BRANDS, PermissionAction.CREATE);
        return ApiResponse.ok("Brand created", catalogService.createBrand(request));
    }

    @PutMapping("/brands/{id}")
    public ApiResponse<Brand> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        requirePermission(currentAdmin(), Module.BRANDS, PermissionAction.UPDATE);
        return ApiResponse.ok("Brand updated", catalogService.saveBrand(request, id));
    }

    @DeleteMapping("/brands/{id}")
    public ApiResponse<Object> deleteBrand(@PathVariable Long id) {
        requirePermission(currentAdmin(), Module.BRANDS, PermissionAction.DELETE);
        catalogService.deleteBrand(id);
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
