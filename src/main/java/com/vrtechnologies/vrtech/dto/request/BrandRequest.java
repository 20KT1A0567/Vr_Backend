package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequest {

    @NotBlank
    private String name;

    private String logoUrl;

    private String description;

    private Integer sortOrder;

    private Integer discountPercent;

    private Boolean active;
}
