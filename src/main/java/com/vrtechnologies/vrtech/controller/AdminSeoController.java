package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.SeoSettingRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.SeoSettingResponse;
import com.vrtechnologies.vrtech.service.SeoSettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seo-settings")
public class AdminSeoController {

    private final SeoSettingService seoSettingService;

    public AdminSeoController(SeoSettingService seoSettingService) {
        this.seoSettingService = seoSettingService;
    }

    @GetMapping
    public ApiResponse<List<SeoSettingResponse>> list() {
        return ApiResponse.ok("SEO settings fetched", seoSettingService.list());
    }

    @PutMapping
    public ApiResponse<SeoSettingResponse> save(@Valid @RequestBody SeoSettingRequest request) {
        return ApiResponse.ok("SEO settings updated", seoSettingService.save(request));
    }
}
