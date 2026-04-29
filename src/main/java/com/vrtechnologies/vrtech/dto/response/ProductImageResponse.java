package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private String publicId;
    private boolean primaryImage;
    private Integer sortOrder;
}
