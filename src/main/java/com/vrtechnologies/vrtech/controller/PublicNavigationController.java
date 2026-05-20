package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.NavigationConfigResponse;
import com.vrtechnologies.vrtech.service.NavigationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/navigation")
public class PublicNavigationController {

    private final NavigationService navigationService;

    public PublicNavigationController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @GetMapping
    public ApiResponse<NavigationConfigResponse> navigation() {
        return ApiResponse.ok("Navigation config fetched", navigationService.getPublicConfig());
    }
}
