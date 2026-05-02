package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.config.JsonMapConverter;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.entity.enums.ProductStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 100)
    private String modelNumber;

    @Column(length = 150)
    private String processor;

    @Column(length = 100)
    private String processorGeneration;

    private Integer ramGb;

    private Integer storageGb;

    @Column(length = 50)
    private String storageType;

    @Column(length = 50)
    private String displaySize;

    @Column(length = 100)
    private String displayType;

    @Column(length = 100)
    private String os;

    @Column(length = 150)
    private String graphicsCard;

    @Column(length = 100)
    private String battery;

    @Column(length = 50)
    private String weight;

    private Integer warrantyMonths;

    @Column(length = 500)
    private String warrantySummary;

    private Integer returnDays;

    @Column(length = 100)
    private String sku;

    @Column(length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    private ProductCondition productCondition;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductStatus productStatus;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    private Integer discountPercent;

    @Column(nullable = false)
    private Integer stockQuantity = 1;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean featured = false;

    private Boolean bestSeller;

    private Boolean todayDeal;

    private LocalDateTime dealStartDate;

    private LocalDateTime dealEndDate;

    private Integer displayOrder;

    @Column(length = 500)
    private String videoUrl;

    @Column(length = 255)
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    @Column(length = 500)
    private String seoKeywords;

    private Integer lowStockThreshold;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, Object> customAttributes = new LinkedHashMap<>();

    @ManyToMany
    @JoinTable(
            name = "product_stores",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    private Set<Store> stores = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private Set<ProductImage> images = new LinkedHashSet<>();

    public ProductStatus getEffectiveProductStatus() {
        return productStatus == null ? ProductStatus.ACTIVE : productStatus;
    }

    public boolean isBestSellerEnabled() {
        return Boolean.TRUE.equals(bestSeller);
    }

    public boolean isTodayDealEnabled() {
        return Boolean.TRUE.equals(todayDeal);
    }

    public int getResolvedDisplayOrder() {
        return displayOrder == null ? 0 : displayOrder;
    }

    public int getResolvedLowStockThreshold() {
        return lowStockThreshold == null ? 5 : lowStockThreshold;
    }
}
