package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.BackInStockRequestDto;
import com.vrtechnologies.vrtech.dto.request.PriceDropAlertRequest;
import com.vrtechnologies.vrtech.dto.request.RecentProductViewRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.BackInStockRequestResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.dto.response.ProductReviewResponse;
import com.vrtechnologies.vrtech.entity.PriceDropAlert;
import com.vrtechnologies.vrtech.service.BackInStockService;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.service.ProductEngagementService;
import com.vrtechnologies.vrtech.service.ProductReviewService;
import com.vrtechnologies.vrtech.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final BackInStockService backInStockService;
    private final ProductEngagementService productEngagementService;
    private final ProductReviewService productReviewService;

    public ProductController(ProductService productService, BackInStockService backInStockService, ProductEngagementService productEngagementService, ProductReviewService productReviewService) {
        this.productService = productService;
        this.backInStockService = backInStockService;
        this.productEngagementService = productEngagementService;
        this.productReviewService = productReviewService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) List<Long> brandIds,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Integer ramGb,
            @RequestParam(required = false) List<Integer> ramOptions,
            @RequestParam(required = false) Integer storageGb,
            @RequestParam(required = false) List<Integer> storageOptions,
            @RequestParam(required = false) String processor,
            @RequestParam(required = false) List<String> processorOptions,
            @RequestParam(required = false) List<String> osOptions,
            @RequestParam(required = false) List<String> displaySizeOptions,
            @RequestParam(required = false) List<String> displayTypeOptions,
            @RequestParam(required = false) List<String> graphicsOptions,
            @RequestParam(required = false) ProductCondition condition,
            @RequestParam(required = false) List<ProductCondition> conditions,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean bestSeller,
            @RequestParam(required = false) Boolean todayDeal,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return ApiResponse.ok(
                "Products fetched",
                productService.getPublicProducts(
                        q,
                        mergeParamValues(brandId, brandIds),
                        mergeParamValues(categoryId, categoryIds),
                        storeId,
                        mergeParamValues(ramGb, ramOptions),
                        mergeParamValues(storageGb, storageOptions),
                        mergeParamValues(processor, processorOptions),
                        osOptions,
                        displaySizeOptions,
                        displayTypeOptions,
                        graphicsOptions,
                        mergeParamValues(condition, conditions),
                        inStock,
                        featured,
                        bestSeller,
                        todayDeal,
                        minPrice,
                        maxPrice
                )
        );
    }

    @GetMapping("/featured")
    public ApiResponse<List<ProductResponse>> featured() {
        return ApiResponse.ok("Featured products", productService.getFeaturedProducts());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.ok("Product fetched", productService.getProduct(id, false));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<ProductReviewResponse>> getProductReviews(@PathVariable Long id) {
        return ApiResponse.ok("Product reviews fetched", productReviewService.getApprovedProductReviews(id));
    }

    @PostMapping("/back-in-stock")
    public ApiResponse<BackInStockRequestResponse> backInStock(@Valid @RequestBody BackInStockRequestDto request) {
        return ApiResponse.ok("Back-in-stock request saved", backInStockService.create(request));
    }

    @PostMapping("/recently-viewed")
    public ApiResponse<Object> recordRecentView(@Valid @RequestBody RecentProductViewRequest request) {
        productEngagementService.recordView(request);
        return ApiResponse.ok("Product view recorded", null);
    }

    @GetMapping("/recently-viewed")
    public ApiResponse<List<ProductResponse>> recentViews(@RequestParam(required = false) String anonymousId) {
        return ApiResponse.ok("Recently viewed products fetched", productEngagementService.recent(anonymousId));
    }

    @PostMapping("/price-drop-alerts")
    public ApiResponse<PriceDropAlert> priceDropAlert(@Valid @RequestBody PriceDropAlertRequest request) {
        return ApiResponse.ok("Price-drop alert saved", productEngagementService.createPriceDropAlert(request));
    }

    private <T> List<T> mergeParamValues(T singleValue, List<T> multipleValues) {
        LinkedHashSet<T> merged = new LinkedHashSet<>();
        if (singleValue != null) {
            merged.add(singleValue);
        }
        if (multipleValues != null) {
            merged.addAll(multipleValues);
        }
        return merged.isEmpty() ? null : new ArrayList<>(merged);
    }
}
