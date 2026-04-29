package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import java.util.LinkedHashSet;
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

    @Column(length = 500)
    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

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
}
