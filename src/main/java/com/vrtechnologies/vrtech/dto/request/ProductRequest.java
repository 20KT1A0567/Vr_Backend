package com.vrtechnologies.vrtech.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.entity.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    private String title;

    @NotNull
    private Long brandId;

    @NotNull
    private Long categoryId;

    @NotEmpty
    private List<Long> storeIds;

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

    @NotNull
    private BigDecimal price;

    private BigDecimal originalPrice;

    @JsonAlias("discountPercentage")
    private Integer discountPercent;

    private Integer stockQuantity;
    private Boolean available;

    @JsonAlias("isFeatured")
    private Boolean featured;

    @JsonAlias("isBestSeller")
    private Boolean bestSeller;

    @JsonAlias("isTodayDeal")
    private Boolean todayDeal;
    private LocalDateTime dealStartDate;
    private LocalDateTime dealEndDate;
    private Integer displayOrder;
    private String videoUrl;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private String hsnCode;
    private BigDecimal gstRatePercent;
    private Boolean taxable;
    private Integer lowStockThreshold;
    private Integer leadTimeDays;
    private String description;
    private Map<String, Object> customAttributes;

    @Valid
    @Size(max = 20)
    private List<ProductImageRequest> images;
}
