package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.entity.Banner;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/brands")
    public ApiResponse<List<Brand>> brands() {
        return ApiResponse.ok("Brands fetched", catalogService.getBrands());
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok("Categories fetched", catalogService.getCategories());
    }

    @GetMapping("/api/stores")
    public ApiResponse<List<Store>> stores() {
        return ApiResponse.ok("Stores fetched", catalogService.getStores());
    }

    @GetMapping("/api/banners")
    public ApiResponse<List<Banner>> banners() {
        return ApiResponse.ok("Banners fetched", catalogService.getBanners());
    }
}
