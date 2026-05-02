package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.ProductSectionRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.ProductSectionResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.service.PermissionService;
import com.vrtechnologies.vrtech.service.ProductSectionService;
import com.vrtechnologies.vrtech.service.UserContextService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product-sections")
public class AdminProductSectionController {

    private final ProductSectionService productSectionService;
    private final PermissionService permissionService;
    private final UserContextService userContextService;

    public AdminProductSectionController(
            ProductSectionService productSectionService,
            PermissionService permissionService,
            UserContextService userContextService
    ) {
        this.productSectionService = productSectionService;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ApiResponse<List<ProductSectionResponse>> sections() {
        requireSectionPermission(currentAdmin(), PermissionAction.VIEW);
        return ApiResponse.ok("Product sections fetched", productSectionService.getAllSections());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductSectionResponse> section(@PathVariable Long id) {
        requireSectionPermission(currentAdmin(), PermissionAction.VIEW);
        return ApiResponse.ok("Product section fetched", productSectionService.getSection(id));
    }

    @PostMapping
    public ApiResponse<ProductSectionResponse> create(@Valid @RequestBody ProductSectionRequest request) {
        requireSectionPermission(currentAdmin(), PermissionAction.CREATE);
        return ApiResponse.ok("Product section created", productSectionService.saveSection(request, null));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductSectionResponse> update(@PathVariable Long id, @Valid @RequestBody ProductSectionRequest request) {
        requireSectionPermission(currentAdmin(), PermissionAction.UPDATE);
        return ApiResponse.ok("Product section updated", productSectionService.saveSection(request, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        requireSectionPermission(currentAdmin(), PermissionAction.DELETE);
        productSectionService.deleteSection(id);
        return ApiResponse.ok("Product section deleted", null);
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }

    private void requireSectionPermission(User admin, PermissionAction action) {
        boolean allowed = permissionService.hasPermission(admin, Module.WEBSITE_CONTENT, action)
                || permissionService.hasPermission(admin, Module.PRODUCTS, action);
        if (!allowed) {
            throw new AccessDeniedException("Missing permission to manage product sections");
        }
    }
}
