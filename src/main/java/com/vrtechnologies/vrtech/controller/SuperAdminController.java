package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.AdminCreateRequest;
import com.vrtechnologies.vrtech.dto.request.AdminPasswordResetRequest;
import com.vrtechnologies.vrtech.dto.request.AdminPermissionsRequest;
import com.vrtechnologies.vrtech.dto.request.AdminRoleCreateRequest;
import com.vrtechnologies.vrtech.dto.request.AdminRoleUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.AdminStatusRequest;
import com.vrtechnologies.vrtech.dto.request.AdminStoresRequest;
import com.vrtechnologies.vrtech.dto.request.AdminUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.RolePermissionsRequest;
import com.vrtechnologies.vrtech.dto.response.AdminPermissionResponse;
import com.vrtechnologies.vrtech.dto.response.AdminLoginHistoryResponse;
import com.vrtechnologies.vrtech.dto.response.AdminUserResponse;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.PageResponse;
import com.vrtechnologies.vrtech.dto.response.PermissionCatalogResponse;
import com.vrtechnologies.vrtech.dto.response.RolePermissionsResponse;
import com.vrtechnologies.vrtech.entity.AdminActivityLog;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import com.vrtechnologies.vrtech.service.SuperAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.vrtechnologies.vrtech.service.UserContextService;
import com.vrtechnologies.vrtech.entity.User;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final StoreRepository storeRepository;
    private final UserContextService userContextService;

    public SuperAdminController(
            SuperAdminService superAdminService,
            StoreRepository storeRepository,
            UserContextService userContextService
    ) {
        this.superAdminService = superAdminService;
        this.storeRepository = storeRepository;
        this.userContextService = userContextService;
    }

    @GetMapping("/admins")
    public ApiResponse<PageResponse<AdminUserResponse>> listAdmins(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        return ApiResponse.ok("Admins fetched", PageResponse.from(superAdminService.listAdmins(role, search, pageable)));
    }

    @PostMapping("/admins")
    public ApiResponse<AdminUserResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        return ApiResponse.ok("Admin created", superAdminService.createAdmin(request));
    }

    @GetMapping("/admins/{id}")
    public ApiResponse<AdminUserResponse> getAdmin(@PathVariable Long id) {
        return ApiResponse.ok("Admin fetched", superAdminService.getAdmin(id));
    }

    @PutMapping("/admins/{id}")
    public ApiResponse<AdminUserResponse> updateAdmin(@PathVariable Long id, @Valid @RequestBody AdminUpdateRequest request) {
        return ApiResponse.ok("Admin updated", superAdminService.updateAdmin(id, request));
    }

    @PatchMapping("/admins/{id}/status")
    public ApiResponse<AdminUserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminStatusRequest request) {
        return ApiResponse.ok("Admin status updated", superAdminService.setStatus(id, request.getStatus()));
    }

    @PatchMapping("/admins/{id}/reset-password")
    public ApiResponse<Object> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordResetRequest request) {
        superAdminService.resetPassword(id, request);
        return ApiResponse.ok("Password reset", null);
    }

    @DeleteMapping("/admins/{id}")
    public ApiResponse<Object> deleteAdmin(@PathVariable Long id) {
        superAdminService.deleteAdmin(id);
        return ApiResponse.ok("Admin disabled", null);
    }

    @GetMapping("/admins/{id}/permissions")
    public ApiResponse<AdminPermissionResponse> getPermissions(@PathVariable Long id) {
        return ApiResponse.ok("Admin permissions fetched", superAdminService.getPermissions(id));
    }

    @PutMapping("/admins/{id}/permissions")
    public ApiResponse<AdminPermissionResponse> updatePermissions(@PathVariable Long id, @RequestBody AdminPermissionsRequest request) {
        return ApiResponse.ok("Admin permissions updated", superAdminService.setPermissions(id, request));
    }

    @PutMapping("/admins/{id}/stores")
    public ApiResponse<AdminUserResponse> updateStores(@PathVariable Long id, @RequestBody AdminStoresRequest request) {
        return ApiResponse.ok("Admin store access updated", superAdminService.setStores(id, request));
    }

    @GetMapping("/admins/{id}/activity-logs")
    public ApiResponse<PageResponse<AdminActivityLog>> adminActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ApiResponse.ok("Admin activity fetched",
                PageResponse.from(superAdminService.getAdminActivity(id, pageable)));
    }

    @GetMapping("/login-history")
    public ApiResponse<PageResponse<AdminLoginHistoryResponse>> loginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "loginAt"));
        return ApiResponse.ok("Login history fetched", PageResponse.from(superAdminService.getLoginHistory(pageable)));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RolePermissionsResponse>> listRoles() {
        return ApiResponse.ok("Roles fetched", superAdminService.allRolePermissions());
    }

    @PostMapping("/roles")
    public ApiResponse<RolePermissionsResponse> createRole(@Valid @RequestBody AdminRoleCreateRequest request) {
        return ApiResponse.ok("Role created", superAdminService.createRole(request));
    }

    @PutMapping("/roles/{roleKey}")
    public ApiResponse<RolePermissionsResponse> updateRole(@PathVariable String roleKey, @Valid @RequestBody AdminRoleUpdateRequest request) {
        User admin = currentAdmin();
        if (RealtimeAdminController.isResourceHardLocked("role:" + roleKey, admin.getEmail())) {
            return ApiResponse.error("This role archetype is hard-locked by another administrator.", null);
        }
        return ApiResponse.ok("Role updated", superAdminService.updateRole(roleKey, request));
    }

    @DeleteMapping("/roles/{roleKey}")
    public ApiResponse<Object> deleteRole(@PathVariable String roleKey) {
        User admin = currentAdmin();
        if (RealtimeAdminController.isResourceHardLocked("role:" + roleKey, admin.getEmail())) {
            return ApiResponse.error("This role archetype is hard-locked by another administrator.", null);
        }
        superAdminService.deleteRole(roleKey);
        return ApiResponse.ok("Role deleted", null);
    }

    @GetMapping("/roles/{roleKey}/permissions")
    public ApiResponse<RolePermissionsResponse> rolePermissions(@PathVariable String roleKey) {
        return ApiResponse.ok("Role permissions fetched", superAdminService.getRolePermissions(roleKey));
    }

    @PutMapping("/roles/{roleKey}/permissions")
    public ApiResponse<RolePermissionsResponse> updateRolePermissions(@PathVariable String roleKey, @RequestBody RolePermissionsRequest request) {
        User admin = currentAdmin();
        if (RealtimeAdminController.isResourceHardLocked("role:" + roleKey, admin.getEmail())) {
            return ApiResponse.error("This role archetype is hard-locked by another administrator.", null);
        }
        return ApiResponse.ok("Role permissions updated", superAdminService.setRolePermissions(roleKey, request));
    }

    @GetMapping("/permissions")
    public ApiResponse<PermissionCatalogResponse> permissionCatalog() {
        return ApiResponse.ok("Permission catalog", superAdminService.permissionCatalog());
    }

    @GetMapping("/stores")
    public ApiResponse<List<Store>> stores() {
        return ApiResponse.ok("Stores fetched", storeRepository.findAllByOrderByCityAscNameAsc());
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }
}
