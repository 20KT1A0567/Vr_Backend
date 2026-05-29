package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.ProductBulkActionRequest;
import com.vrtechnologies.vrtech.dto.request.ProductRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.AuditLogEntryResponse;
import com.vrtechnologies.vrtech.dto.response.PageResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.dto.response.ProductImportResponse;
import com.vrtechnologies.vrtech.dto.response.LowStockPredictionResponse;
import com.vrtechnologies.vrtech.service.InventoryPredictionService;
import com.vrtechnologies.vrtech.entity.AdminActivityLog;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.ProductBulkActionType;
import com.vrtechnologies.vrtech.service.AdminActivityLogService;
import com.vrtechnologies.vrtech.service.PermissionService;
import com.vrtechnologies.vrtech.service.ProductService;
import com.vrtechnologies.vrtech.service.ProductImportExportService;
import com.vrtechnologies.vrtech.service.UserContextService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final PermissionService permissionService;
    private final UserContextService userContextService;
    private final ProductImportExportService productImportExportService;
    private final AdminActivityLogService activityLogService;
    private final InventoryPredictionService inventoryPredictionService;

    public AdminProductController(
            ProductService productService,
            PermissionService permissionService,
            UserContextService userContextService,
            ProductImportExportService productImportExportService,
            AdminActivityLogService activityLogService,
            InventoryPredictionService inventoryPredictionService
    ) {
        this.productService = productService;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
        this.productImportExportService = productImportExportService;
        this.activityLogService = activityLogService;
        this.inventoryPredictionService = inventoryPredictionService;
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

    @GetMapping("/predictions")
    public ApiResponse<List<LowStockPredictionResponse>> lowStockPredictions() {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.VIEW);
        return ApiResponse.ok("Low stock predictions fetched", inventoryPredictionService.getPredictions());
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
        if (RealtimeAdminController.isResourceHardLocked("product:" + id, admin.getEmail())) {
            return ApiResponse.error("This product is hard-locked by another administrator editing it.", null);
        }
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts() {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.EXPORT);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(productImportExportService.exportCsv(admin));
    }

    @PostMapping("/import")
    public ApiResponse<ProductImportResponse> importProducts(@RequestParam("file") MultipartFile file) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.CREATE);
        return ApiResponse.ok("Product import completed", productImportExportService.importCsv(file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.DELETE);
        if (RealtimeAdminController.isResourceHardLocked("product:" + id, admin.getEmail())) {
            return ApiResponse.error("This product is hard-locked by another administrator editing it.", null);
        }
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
        if (RealtimeAdminController.isResourceHardLocked("product:" + id, admin.getEmail())) {
            return ApiResponse.error("This product is hard-locked by another administrator editing it.", null);
        }
        return ApiResponse.ok("Image uploaded", productService.uploadProductImage(admin, id, file));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ApiResponse<ProductResponse> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.UPDATE);
        if (RealtimeAdminController.isResourceHardLocked("product:" + id, admin.getEmail())) {
            return ApiResponse.error("This product is hard-locked by another administrator editing it.", null);
        }
        return ApiResponse.ok("Image deleted", productService.deleteProductImage(admin, id, imageId));
    }

    @GetMapping("/{id}/audit")
    public ApiResponse<PageResponse<AuditLogEntryResponse>> auditHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User admin = currentAdmin();
        permissionService.requirePermission(admin, Module.PRODUCTS, PermissionAction.VIEW);
        // Throws ResourceNotFoundException if missing or not visible to this admin
        productService.getAdminProduct(admin, id);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<AdminActivityLog> logs = activityLogService.forEntity("Product", id, pageable);
        return ApiResponse.ok("Product audit fetched", PageResponse.from(logs.map(this::toAuditResponse)));
    }

    private AuditLogEntryResponse toAuditResponse(AdminActivityLog entry) {
        return AuditLogEntryResponse.builder()
                .id(entry.getId())
                .adminId(entry.getAdminId())
                .adminEmail(entry.getAdminEmail())
                .module(entry.getModule() != null ? entry.getModule().name() : null)
                .action(entry.getAction() != null ? entry.getAction().name() : null)
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .oldValue(entry.getOldValue())
                .newValue(entry.getNewValue())
                .description(entry.getDescription())
                .ipAddress(entry.getIpAddress())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }
}
