package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.AttributeRequest;
import com.vrtechnologies.vrtech.dto.request.AttributeValueRequest;
import com.vrtechnologies.vrtech.dto.request.BulkVariantsRequest;
import com.vrtechnologies.vrtech.dto.request.CreateProductVariantRequest;
import com.vrtechnologies.vrtech.dto.request.UpdateProductVariantRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.AttributeResponse;
import com.vrtechnologies.vrtech.dto.response.ProductVariantResponse;
import com.vrtechnologies.vrtech.service.ProductVariantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping("/admin/variants/attributes")
    public ApiResponse<List<AttributeResponse>> getAllAttributes() {
        return ApiResponse.ok("Attributes fetched successfully", productVariantService.getAllAttributes());
    }

    @PostMapping("/admin/variants/attributes")
    public ApiResponse<AttributeResponse> createAttribute(@RequestBody AttributeRequest request) {
        return ApiResponse.ok("Attribute created successfully", productVariantService.createAttribute(request));
    }

    @DeleteMapping("/admin/variants/attributes/{id}")
    public ApiResponse<Void> deleteAttribute(@PathVariable Long id) {
        productVariantService.deleteAttribute(id);
        return ApiResponse.ok("Attribute deleted successfully", null);
    }

    @PostMapping("/admin/variants/attributes/{id}/values")
    public ApiResponse<AttributeResponse.AttributeValueResponse> createAttributeValue(
            @PathVariable Long id,
            @RequestBody AttributeValueRequest request
    ) {
        return ApiResponse.ok("Attribute value created successfully", productVariantService.createAttributeValue(id, request));
    }

    @DeleteMapping("/admin/variants/attribute-values/{id}")
    public ApiResponse<Void> deleteAttributeValue(@PathVariable Long id) {
        productVariantService.deleteAttributeValue(id);
        return ApiResponse.ok("Attribute value deleted successfully", null);
    }

    @GetMapping("/admin/variants/products/{productId}/variants")
    public ApiResponse<List<ProductVariantResponse>> getVariantsForProduct(@PathVariable Long productId) {
        return ApiResponse.ok("Product variants fetched successfully", productVariantService.getVariantsForProduct(productId));
    }

    @PostMapping("/admin/variants/products/{productId}/variants")
    public ApiResponse<ProductVariantResponse> createProductVariant(
            @PathVariable Long productId,
            @RequestBody CreateProductVariantRequest request
    ) {
        return ApiResponse.ok("Product variant created successfully", productVariantService.createProductVariant(productId, request));
    }

    @PutMapping("/admin/variants/{variantId}")
    public ApiResponse<ProductVariantResponse> updateProductVariant(
            @PathVariable Long variantId,
            @RequestBody UpdateProductVariantRequest request
    ) {
        return ApiResponse.ok("Product variant updated successfully", productVariantService.updateProductVariant(variantId, request));
    }

    @DeleteMapping("/admin/variants/{variantId}")
    public ApiResponse<Void> deleteProductVariant(@PathVariable Long variantId) {
        productVariantService.deleteProductVariant(variantId);
        return ApiResponse.ok("Product variant deleted successfully", null);
    }

    @GetMapping("/products/{productId}/variants")
    public ApiResponse<List<ProductVariantResponse>> getVariantsForProductPublic(@PathVariable Long productId) {
        return ApiResponse.ok("Product variants fetched successfully", productVariantService.getVariantsForProduct(productId));
    }

    @GetMapping("/products/{productId}/variant")
    public ApiResponse<ProductVariantResponse> queryVariant(
            @PathVariable Long productId,
            @RequestParam Map<String, String> params
    ) {
        return ApiResponse.ok("Product variant resolved successfully", productVariantService.queryVariant(productId, params));
    }
}
