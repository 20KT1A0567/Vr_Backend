package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.CmsPageRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.CmsPageResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.service.CmsPageService;
import com.vrtechnologies.vrtech.service.PermissionService;
import com.vrtechnologies.vrtech.service.UserContextService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cms-pages")
public class AdminCmsPageController {

    private final CmsPageService cmsPageService;
    private final PermissionService permissionService;
    private final UserContextService userContextService;

    public AdminCmsPageController(
            CmsPageService cmsPageService,
            PermissionService permissionService,
            UserContextService userContextService
    ) {
        this.cmsPageService = cmsPageService;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
    }

    @GetMapping
    public ApiResponse<List<CmsPageResponse>> pages() {
        permissionService.requirePermission(currentAdmin(), Module.WEBSITE_CONTENT, PermissionAction.VIEW);
        return ApiResponse.ok("CMS pages fetched", cmsPageService.getAllPages());
    }

    @GetMapping("/{slug}")
    public ApiResponse<CmsPageResponse> page(@PathVariable String slug) {
        permissionService.requirePermission(currentAdmin(), Module.WEBSITE_CONTENT, PermissionAction.VIEW);
        return ApiResponse.ok("CMS page fetched", cmsPageService.getPage(slug, true));
    }

    @PutMapping("/{slug}")
    public ApiResponse<CmsPageResponse> update(@PathVariable String slug, @Valid @RequestBody CmsPageRequest request) {
        permissionService.requirePermission(currentAdmin(), Module.WEBSITE_CONTENT, PermissionAction.UPDATE);
        return ApiResponse.ok("CMS page updated", cmsPageService.savePage(slug, request));
    }

    private User currentAdmin() {
        return userContextService.getCurrentUser();
    }
}
