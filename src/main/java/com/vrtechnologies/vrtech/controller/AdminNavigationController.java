package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.NavigationConfigRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.NavigationConfigResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.service.NavigationService;
import com.vrtechnologies.vrtech.service.PermissionService;
import com.vrtechnologies.vrtech.service.UserContextService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/navigation")
public class AdminNavigationController {

    private final NavigationService navigationService;
    private final PermissionService permissionService;
    private final UserContextService userContextService;

    public AdminNavigationController(
            NavigationService navigationService,
            PermissionService permissionService,
            UserContextService userContextService
    ) {
        this.navigationService = navigationService;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ApiResponse<NavigationConfigResponse> navigation() {
        permissionService.requirePermission(currentAdmin(), Module.WEBSITE_CONTENT, PermissionAction.VIEW);
        return ApiResponse.ok("Navigation config fetched", navigationService.getAdminConfig());
    }

    @PutMapping
    public ApiResponse<NavigationConfigResponse> update(@Valid @RequestBody NavigationConfigRequest request) {
        permissionService.requirePermission(currentAdmin(), Module.WEBSITE_CONTENT, PermissionAction.UPDATE);
        return ApiResponse.ok("Navigation config updated", navigationService.saveConfig(request));
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }
}
