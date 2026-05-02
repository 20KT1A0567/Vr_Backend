package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.ProductSectionSelectionMode;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "product_sections")
public class ProductSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductSectionType sectionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductSectionSelectionMode selectionMode = ProductSectionSelectionMode.AUTOMATIC;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Column(nullable = false)
    private Integer maxProducts = 8;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private Set<ProductSectionProduct> products = new LinkedHashSet<>();
}
