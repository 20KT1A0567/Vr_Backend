package com.vrtechnologies.vrtech.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionSelectionMode;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProductSectionRequest {

    @NotBlank
    private String title;

    private String subtitle;

    @NotNull
    private ProductSectionType sectionType;

    private ProductSectionSelectionMode selectionMode;

    private Integer displayOrder;

    private Boolean active;

    @JsonAlias("startDate")
    private LocalDateTime startAt;

    @JsonAlias("endDate")
    private LocalDateTime endAt;

    @Min(1)
    private Integer maxProducts;

    @Valid
    @Size(max = 100)
    private List<ProductSectionItemRequest> products;

    @Size(max = 100)
    private List<Long> productIds;
}
