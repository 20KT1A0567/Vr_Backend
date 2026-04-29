package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

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

    @NotNull
    private BigDecimal price;

    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Integer stockQuantity;
    private Boolean available;
    private Boolean featured;
    private String videoUrl;
    private String description;

    @Valid
    @Size(max = 20)
    private List<ProductImageRequest> images;
}
