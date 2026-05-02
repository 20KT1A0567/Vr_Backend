package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSectionItemResponse {

    private Long id;
    private Integer displayOrder;
    private ProductResponse product;
}
