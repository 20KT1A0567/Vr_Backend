package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.ProductBulkActionRequest;
import com.vrtechnologies.vrtech.dto.request.ProductRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.ProductBulkActionType;
import com.vrtechnologies.vrtech.service.PermissionService;
import com.vrtechnologies.vrtech.service.ProductService;
import com.vrtechnologies.vrtech.service.UserContextService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final PermissionService permissionService;
    private final UserContextService userContextService;

    public AdminProductController(
            ProductService productService,
            PermissionService permissionService,
            UserContextService userContextService
    ) {
        this.productService = productService;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> allProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Long> brandIds,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> storeIds,
            @RequestParam(required = false) List<String> stockStates,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean bestSeller,
            @RequestParam(required = false) Boolean todayDeal,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.VIEW);
        return ApiResponse.ok("Admin products fetched", productService.getAllProductsForAdmin(
                admin,
                q,
                brandIds,
                categoryIds,
                storeIds,
                stockStates,
                available,
                featured,
                bestSeller,
                todayDeal,
                minPrice,
                maxPrice
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> product(@PathVariable Long id) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.VIEW);
        return ApiResponse.ok("Product fetched", productService.getAdminProduct(admin, id));
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.CREATE);
        return ApiResponse.ok("Product created", productService.createProduct(admin, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.UPDATE);
        return ApiResponse.ok("Product updated", productService.updateProduct(admin, id, request));
    }

    @PatchMapping("/bulk")
    public ApiResponse<Object> bulkAction(@Valid @RequestBody ProductBulkActionRequest request) {
        User admin = currentAdmin();
        PermissionAction action = request.getAction() == null || request.getAction() == ProductBulkActionType.DELETE
                ? PermissionAction.DELETE
                : PermissionAction.UPDATE;
        permissionService.requirePermission(admin, Module.PRODUCTS, action);
        productService.applyBulkAction(admin, request);
        return ApiResponse.ok("Bulk product action completed", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.DELETE);
        productService.deleteProduct(admin, id);
        return ApiResponse.ok("Product deleted", null);
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<ProductResponse> duplicate(@PathVariable Long id) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.CREATE);
        return ApiResponse.ok("Product duplicated", productService.duplicateProduct(admin, id));
    }

    @PostMapping("/{id}/images")
    public ApiResponse<ProductResponse> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.UPDATE);
        return ApiResponse.ok("Image uploaded", productService.uploadProductImage(admin, id, file));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ApiResponse<ProductResponse> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.UPDATE);
        return ApiResponse.ok("Image deleted", productService.deleteProductImage(admin, id, imageId));
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }
}
