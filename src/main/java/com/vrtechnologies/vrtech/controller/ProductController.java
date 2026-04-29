package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public ProductController(ProductService productService) {
        this.productService = productService;
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
            @RequestParam(required = false) ProductCondition condition,
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
                        condition,
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
