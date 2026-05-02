package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class HomeSectionResponse {

    private Long id;
    private String title;
    private String subtitle;
    private ProductSectionType sectionType;
    private Integer displayOrder;
    private Integer maxProducts;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private List<ProductResponse> products;
}
