package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String title;
    private Long brandId;
    private String brandName;
    private String brandLogoUrl;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String modelNumber;
    private String processor;
    private String processorGeneration;
    private Integer ramGb;
    private Integer storageGb;
    private String storageType;
    private String displaySize;
    private String displayType;
    private String os;
    private String graphicsCard;
    private String battery;
    private String weight;
    private Integer warrantyMonths;
    private String warrantySummary;
    private Integer returnDays;
    private String sku;
    private String serialNumber;
    private ProductCondition productCondition;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Integer stockQuantity;
    private boolean available;
    private boolean featured;
    private String videoUrl;
    private String description;
    private List<StoreSummaryResponse> stores;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
