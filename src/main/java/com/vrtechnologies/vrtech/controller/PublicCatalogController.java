package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.BannerResponse;
import com.vrtechnologies.vrtech.dto.response.HomeSectionResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.enums.BannerPlacement;
import com.vrtechnologies.vrtech.service.CatalogService;
import com.vrtechnologies.vrtech.service.ProductSectionService;
import com.vrtechnologies.vrtech.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {

    private final ProductSectionService productSectionService;
    private final ProductService productService;
    private final CatalogService catalogService;

    public PublicCatalogController(
            ProductSectionService productSectionService,
            ProductService productService,
            CatalogService catalogService
    ) {
        this.productSectionService = productSectionService;
        this.productService = productService;
        this.catalogService = catalogService;
    }

    @GetMapping("/home-sections")
    public ApiResponse<List<HomeSectionResponse>> homeSections() {
        return ApiResponse.ok("Home sections fetched", productSectionService.getPublicHomeSections());
    }

    @GetMapping("/products/best-sellers")
    public ApiResponse<List<ProductResponse>> bestSellers(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok("Best sellers fetched", productSectionService.getBestSellers(limit));
    }

    @GetMapping("/products/todays-deals")
    public ApiResponse<List<ProductResponse>> todaysDeals(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok("Today's deals fetched", productService.getTodaysDeals(limit));
    }

    @GetMapping("/products/featured")
    public ApiResponse<List<ProductResponse>> featuredProducts(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok("Featured products fetched", productService.getFeaturedProducts(limit));
    }

    @GetMapping("/products/new-arrivals")
    public ApiResponse<List<ProductResponse>> newArrivals(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok("New arrivals fetched", productService.getNewArrivals(limit));
    }

    @GetMapping("/banners")
    public ApiResponse<List<BannerResponse>> banners(@RequestParam(required = false) BannerPlacement placement) {
        return ApiResponse.ok("Public banners fetched", catalogService.getBanners(false, placement));
    }
}
