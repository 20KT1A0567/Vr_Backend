package com.vrtechnologies.vrtech.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.entity.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private ProductStatus productStatus;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Integer stockQuantity;
    private boolean available;
    private boolean featured;
    private boolean bestSeller;
    private boolean todayDeal;
    private LocalDateTime dealStartDate;
    private LocalDateTime dealEndDate;
    private Integer displayOrder;
    private String videoUrl;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private String hsnCode;
    private BigDecimal gstRatePercent;
    private boolean taxable;
    private Integer lowStockThreshold;
    private Integer leadTimeDays;
    private String description;
    private Map<String, Object> customAttributes;
    private List<StoreSummaryResponse> stores;
    private List<StoreAvailabilityResponse> storeAvailability;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean lowestPrice90Days;

    @JsonProperty("discountPercentage")
    public Integer getDiscountPercentage() {
        return discountPercent;
    }

    @JsonProperty("isFeatured")
    public boolean getIsFeatured() {
        return featured;
    }

    @JsonProperty("isBestSeller")
    public boolean getIsBestSeller() {
        return bestSeller;
    }

    @JsonProperty("isTodayDeal")
    public boolean getIsTodayDeal() {
        return todayDeal;
    }
}
