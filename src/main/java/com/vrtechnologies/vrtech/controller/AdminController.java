package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.BannerRequest;
import com.vrtechnologies.vrtech.dto.request.BrandRequest;
import com.vrtechnologies.vrtech.dto.request.CategoryRequest;
import com.vrtechnologies.vrtech.dto.request.CouponRequest;
import com.vrtechnologies.vrtech.dto.request.SiteSettingsRequest;
import com.vrtechnologies.vrtech.dto.request.StatusUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.StoreRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.DashboardStatsResponse;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.UserSummaryResponse;
import com.vrtechnologies.vrtech.entity.Banner;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Coupon;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.SiteSettings;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.service.AdminService;
import com.vrtechnologies.vrtech.service.CatalogService;
import com.vrtechnologies.vrtech.service.CloudinaryService;
import com.vrtechnologies.vrtech.service.EnquiryService;
import com.vrtechnologies.vrtech.service.OrderService;
import jakarta.validation.Valid;
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

    public AdminController(
            AdminService adminService,
            OrderService orderService,
            EnquiryService enquiryService,
            CatalogService catalogService,
            CloudinaryService cloudinaryService
    ) {
        this.adminService = adminService;
        this.orderService = orderService;
        this.enquiryService = enquiryService;
        this.catalogService = catalogService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsResponse> dashboard() {
        return ApiResponse.ok("Dashboard stats fetched", adminService.getDashboardStats());
    }

    @GetMapping("/brands")
    public ApiResponse<List<Brand>> adminBrands() {
        return ApiResponse.ok("Brands fetched", catalogService.getBrands());
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> adminCategories() {
        return ApiResponse.ok("Categories fetched", catalogService.getCategories());
    }

    @GetMapping("/stores")
    public ApiResponse<List<Store>> adminStores() {
        return ApiResponse.ok("Stores fetched", catalogService.getStores(true));
    }

    @GetMapping("/banners")
    public ApiResponse<List<Banner>> adminBanners() {
        return ApiResponse.ok("Banners fetched", catalogService.getBanners(true));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserSummaryResponse>> users() {
        return ApiResponse.ok("Users fetched", adminService.getUsers());
    }

    @GetMapping("/coupons")
    public ApiResponse<List<Coupon>> coupons() {
        return ApiResponse.ok("Coupons fetched", adminService.getCoupons());
    }

    @PostMapping("/coupons")
    public ApiResponse<Coupon> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok("Coupon created", adminService.saveCoupon(request, null));
    }

    @PutMapping("/coupons/{id}")
    public ApiResponse<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok("Coupon updated", adminService.saveCoupon(request, id));
    }

    @DeleteMapping("/coupons/{id}")
    public ApiResponse<Object> deleteCoupon(@PathVariable Long id) {
        adminService.deleteCoupon(id);
        return ApiResponse.ok("Coupon deleted", null);
    }

    @GetMapping("/settings")
    public ApiResponse<SiteSettings> settings() {
        return ApiResponse.ok("Settings fetched", adminService.getSiteSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<SiteSettings> updateSettings(@Valid @RequestBody SiteSettingsRequest request) {
        return ApiResponse.ok("Settings updated", adminService.saveSiteSettings(request));
    }

    @PatchMapping("/users/{id}/toggle")
    public ApiResponse<UserSummaryResponse> toggleUser(@PathVariable Long id) {
        return ApiResponse.ok("User updated", adminService.toggleUser(id));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> orders() {
        return ApiResponse.ok("Orders fetched", orderService.getAllOrders());
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        OrderStatus status = parseEnum(OrderStatus.class, request.getValue());
        return ApiResponse.ok("Order status updated", orderService.updateOrderStatus(id, status));
    }

    @PatchMapping("/orders/{id}/payment-status")
    public ApiResponse<OrderResponse> updatePaymentStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        PaymentStatus paymentStatus = parseEnum(PaymentStatus.class, request.getValue());
        return ApiResponse.ok("Payment status updated", orderService.updatePaymentStatus(id, paymentStatus));
    }

    @GetMapping("/enquiries")
    public ApiResponse<List<Enquiry>> enquiries() {
        return ApiResponse.ok("Enquiries fetched", enquiryService.getAll());
    }

    @PatchMapping("/enquiries/{id}/status")
    public ApiResponse<Enquiry> updateEnquiryStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        EnquiryStatus status = parseEnum(EnquiryStatus.class, request.getValue());
        return ApiResponse.ok("Enquiry updated", enquiryService.updateStatus(id, status));
    }

    @PostMapping("/stores")
    public ApiResponse<Store> createStore(@Valid @RequestBody StoreRequest request) {
        return ApiResponse.ok("Store created", catalogService.saveStore(request, null));
    }

    @PutMapping("/stores/{id}")
    public ApiResponse<Store> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return ApiResponse.ok("Store updated", catalogService.saveStore(request, id));
    }

    @PostMapping("/banners")
    public ApiResponse<Banner> createBanner(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok("Banner created", catalogService.saveBanner(request, null));
    }

    @PutMapping("/banners/{id}")
    public ApiResponse<Banner> updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok("Banner updated", catalogService.saveBanner(request, id));
    }

    @DeleteMapping("/banners/{id}")
    public ApiResponse<Object> deleteBanner(@PathVariable Long id) {
        catalogService.deleteBanner(id);
        return ApiResponse.ok("Banner deleted", null);
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category created", catalogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated", catalogService.saveCategory(request, id));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Object> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ApiResponse.ok("Category deleted", null);
    }

    @PostMapping("/brands")
    public ApiResponse<Brand> createBrand(@Valid @RequestBody BrandRequest request) {
        return ApiResponse.ok("Brand created", catalogService.createBrand(request));
    }

    @PutMapping("/brands/{id}")
    public ApiResponse<Brand> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.ok("Brand updated", catalogService.saveBrand(request, id));
    }

    @DeleteMapping("/brands/{id}")
    public ApiResponse<Object> deleteBrand(@PathVariable Long id) {
        catalogService.deleteBrand(id);
        return ApiResponse.ok("Brand deleted", null);
    }

    @PostMapping("/media/upload")
    public ApiResponse<MediaUploadResponse> uploadMedia(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "general") String folder) {
        return ApiResponse.ok("Media uploaded", cloudinaryService.uploadImage(file, folder));
    }

    @DeleteMapping("/media")
    public ApiResponse<Object> deleteMedia(@RequestParam String publicId) {
        cloudinaryService.deleteAsset(publicId);
        return ApiResponse.ok("Media deleted", null);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String rawValue) {
        return Enum.valueOf(enumType, rawValue.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    }
}
