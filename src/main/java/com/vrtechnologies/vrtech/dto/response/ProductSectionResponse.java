package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.ProductSectionSelectionMode;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductSectionResponse {

    private Long id;
    private String title;
    private String subtitle;
    private ProductSectionType sectionType;
    private ProductSectionSelectionMode selectionMode;
    private Integer displayOrder;
    private boolean active;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer maxProducts;
    private List<ProductSectionItemResponse> products;
    private List<ProductResponse> resolvedProducts;
}
